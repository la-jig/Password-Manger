import javax.swing.*;

class PopUpMenu extends JPopupMenu {
    JMenuItem anItem;
    public PopUpMenu() {
        anItem = new JMenuItem("Click Me!");
        add(anItem);
    }
}
