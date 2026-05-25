package src;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class HelloSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Hello World");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new JLabel("Hello World!"));
            frame.setSize(300, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
