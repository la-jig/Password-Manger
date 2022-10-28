import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.event.*;
import java.io.File;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;


public class Main {
    public static String input_password;
    public static Connection conn = null;
    public static Statement stmt = null;

    public static void main(String[] args) {

        JFrame window = new JFrame();
        JMenuBar menubar;
        JMenu filemenu;
        JMenu editmenu;

        menubar = new JMenuBar();

        filemenu = new JMenu("File");
        editmenu = new JMenu("Edit");

        menubar.add(filemenu);
        menubar.add(editmenu);

        JMenuItem addMenu = new JMenuItem("Add Entry");
        JMenuItem deleteMenu = new JMenuItem("Delete Entry");
        JMenuItem editMenu = new JMenuItem("Edit Entry");

        JMenuItem genPass = new JMenuItem("Generate Password");


        // Add menus
        filemenu.add(addMenu);
        filemenu.add(deleteMenu);
        filemenu.add(editMenu);

        editmenu.add(genPass);

        window.setJMenuBar(menubar);



        File database = new File("password_vault.db");
        boolean isSetup = false;
        String[][] data = new String[][]{};
        String[] column = new String[]{"ID", "WEBSITE", "USERNAME", "PASSWORD"};
        TableModel tableModel = new DefaultTableModel(column, data.length);
        JTable table = new JTable(tableModel);
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        if (!database.isFile()) {
            isSetup = true;
        }

        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:password_vault.db");
            stmt = conn.createStatement();

            String sql = "CREATE TABLE IF NOT EXISTS vault" +
                    "(id INTEGER PRIMARY KEY," +
                    "website TEXT  NOT NULL," +
                    "username TEXT NOT NULL," +
                    "password TEXT NOT NULL)";

            stmt.executeUpdate(sql);

            sql = "CREATE TABLE IF NOT EXISTS masterpassword" +
            "(password TEXT NOT NULL," +
            "recoverykey TEXT NOT NULL)";

            stmt.executeUpdate(sql);

            if (isSetup) {
                String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijk"
                        +"lmnopqrstuvwxyz!@#$%&";
                StringBuilder sb = new StringBuilder(18);
                Random random = new Random();
                for (int i = 0; i < 18; i++) {
                    sb.append(chars.charAt(random.nextInt(chars.length())));
                }
                input_password = JOptionPane.showInputDialog("Enter a new password: ");

                String recoveryKey = sb.toString();
                sql = "INSERT INTO masterpassword(password, recoverykey)" +
                        "VALUES('" +  EncryptorAesGcmPassword.encrypt(input_password.getBytes(), recoveryKey) + "', '" +  MessageDigest.getInstance("SHA3-256").digest(recoveryKey.getBytes()).toString() + "')";
                stmt.executeUpdate(sql);

                StringSelection strselection = new StringSelection(recoveryKey);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(strselection, null);

                JOptionPane.showMessageDialog(window, "Your recovery key is: " + recoveryKey + "\nIt's has been copied to clipboard!", "Setup", JOptionPane.INFORMATION_MESSAGE);
            } else {
                input_password = JOptionPane.showInputDialog("Enter master password: ");

                if (input_password == "") {
                    String recovery_key = JOptionPane.showInputDialog("Enter recovery key: ");


                }
            }
            ResultSet passwords = stmt.executeQuery("SELECT * FROM vault");
            while (passwords.next()) {
                String id = passwords.getString("id");
                String website = EncryptorAesGcmPassword.decrypt(passwords.getString("website"), input_password);
                String username = EncryptorAesGcmPassword.decrypt(passwords.getString("username"), input_password);
                String password = EncryptorAesGcmPassword.decrypt(passwords.getString("password"), input_password);
                model.addRow(new Object[]{id, website, username, password});
            }
        } catch (javax.crypto.AEADBadTagException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(window, "Incorrect password", "ERROR", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(window, e.getClass() + ": " + e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        table.setBounds(30, 40, 200, 300);
        table.addMouseListener(new PopClickListener());
        JScrollPane scrollbar = new JScrollPane(table);
        window.add(scrollbar);
        window.setSize(500, 400);
        window.setResizable(false);
        window.setTitle("JPass");

        genPass.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    int length = Integer.parseInt(JOptionPane.showInputDialog(window, "Enter password length"));

                    String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijk"
                        +"lmnopqrstuvwxyz!@#$%&";
                    StringBuilder sb = new StringBuilder(length);
                    Random random = new Random();
                    for (int i = 0; i < 18; i++) {
                        sb.append(chars.charAt(random.nextInt(chars.length())));
                    }
                    String newPassword = sb.toString();
                    
                    if (JOptionPane.showConfirmDialog(window, "Your password is: " + newPassword + ",\nWould you like to copy it to clipboard?", "", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        StringSelection strselection = new StringSelection(newPassword);

                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(strselection, null);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();

                }
            }
            
        });

        addMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    add(input_password);
                    model.setRowCount(0);
                    ResultSet passwords = stmt.executeQuery("SELECT * FROM vault");
                    while (passwords.next()) {
                        String id = passwords.getString("id");
                        String website = EncryptorAesGcmPassword.decrypt(passwords.getString("website"), input_password);
                        String username = EncryptorAesGcmPassword.decrypt(passwords.getString("username"), input_password);
                        String password = EncryptorAesGcmPassword.decrypt(passwords.getString("password"), input_password);
                        model.addRow(new Object[]{id, website, username, password});
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(window, e.getClass() + ": " + e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            }
        });
        deleteMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    int row = table.getSelectedRow();
                    int column = table.getSelectedColumn();
                    String delete_id = (String) table.getValueAt(row, column);
                    String sql = "DELETE FROM vault WHERE id = " + delete_id;

                    stmt.executeUpdate(sql);

                    model.setRowCount(0);
                    ResultSet passwords = stmt.executeQuery("SELECT * FROM vault");
                    while (passwords.next()) {
                        String id = passwords.getString("id");
                        String website = EncryptorAesGcmPassword.decrypt(passwords.getString("website"), input_password);
                        String username = EncryptorAesGcmPassword.decrypt(passwords.getString("username"), input_password);
                        String password = EncryptorAesGcmPassword.decrypt(passwords.getString("password"), input_password);
                        model.addRow(new Object[]{id, website, username, password});
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(window, e.getClass() + ": " + e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }

            }
        });

        editMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                int row = table.getSelectedRow();
                int column = table.getSelectedColumn();
                String edit_id = (String) table.getValueAt(row, column);
                String new_website = JOptionPane.showInputDialog("Enter new website: ");
                String new_username = JOptionPane.showInputDialog("Enter new username: ");
                String new_password = JOptionPane.showInputDialog("Enter new password: ");


                try {
                    String sql = "UPDATE vault SET 'website' = '" + EncryptorAesGcmPassword.encrypt(new_website.getBytes(), input_password) + "', " + "'username' = '" + EncryptorAesGcmPassword.encrypt(new_username.getBytes(), input_password) + "', " + "'password' = '" + EncryptorAesGcmPassword.encrypt(new_password.getBytes(), input_password) + "' WHERE id = " + edit_id;

                    stmt.executeUpdate(sql);

                    model.setRowCount(0);
                    ResultSet passwords = stmt.executeQuery("SELECT * FROM vault");
                    while (passwords.next()) {
                        String id = passwords.getString("id");
                        String website = EncryptorAesGcmPassword.decrypt(passwords.getString("website"), input_password);
                        String username = EncryptorAesGcmPassword.decrypt(passwords.getString("username"), input_password);
                        String password = EncryptorAesGcmPassword.decrypt(passwords.getString("password"), input_password);
                        model.addRow(new Object[]{id, website, username, password});
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(window, e.getClass() + ": " + e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            }
        });
        
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);
    }

    public static void add(String encryptionPassword) {
        String website = JOptionPane.showInputDialog("Enter website: ");
        String username = JOptionPane.showInputDialog("Enter username: ");
        String password = JOptionPane.showInputDialog("Enter password: ");

        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:password_vault.db");
            stmt = conn.createStatement();
    

            String sql = "INSERT INTO vault(website, username, password)" +
                    "VALUES('" + EncryptorAesGcmPassword.encrypt(website.getBytes(), encryptionPassword) + "', '" + EncryptorAesGcmPassword.encrypt(username.getBytes(), encryptionPassword) + "', '" + EncryptorAesGcmPassword.encrypt(password.getBytes(), encryptionPassword) + "')";

            stmt.executeUpdate(sql);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getClass() + ": " + e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
        }
    }

}

