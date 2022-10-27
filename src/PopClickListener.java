import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

class PopClickListener extends MouseAdapter {
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger())
            doPop(e);
    }

    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger())
            doPop(e);
    }

    private void doPop(MouseEvent e) {
        PopUpMenu menu = new PopUpMenu();
        menu.show(e.getComponent(), e.getX(), e.getY());
    }
}
