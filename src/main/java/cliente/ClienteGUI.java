package cliente;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ClienteGUI {
    private JFrame frame;
    private JTextArea messageArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton fileButton;
    private JButton connectButton;
    private JButton disconnectButton;
    private BufferedReader in;
    private PrintWriter out;
    private Socket socket;
    private String clientName;

    public static void main(String[] args) {
        new ClienteGUI();
    }

    public ClienteGUI() {
        frame = new JFrame("Cliente TCP");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);

        messageArea = new JTextArea();
        messageArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messageArea);

        inputField = new JTextField(30);
        sendButton = new JButton("Enviar");
        fileButton = new JButton("Enviar Archivo");
        connectButton = new JButton("Conectar");
        disconnectButton = new JButton("Desconectar");

        sendButton.setEnabled(false);
        fileButton.setEnabled(false);
        disconnectButton.setEnabled(false);

        sendButton.addActionListener(e -> sendMessage());
        fileButton.addActionListener(e -> sendFile());
        connectButton.addActionListener(e -> connectToServer());
        disconnectButton.addActionListener(e -> disconnectFromServer());

        JPanel panel = new JPanel();
        panel.add(inputField);
        panel.add(sendButton);
        panel.add(fileButton);
        panel.add(connectButton);
        panel.add(disconnectButton);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(panel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 1234);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            clientName = JOptionPane.showInputDialog(frame, "Ingresa tu nombre:");
            out.println(clientName);

            new Thread(new ReaderHandler()).start();
            sendButton.setEnabled(true);
            fileButton.setEnabled(true);
            disconnectButton.setEnabled(true);
            connectButton.setEnabled(false);
            messageArea.append("Conectado al servidor.\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disconnectFromServer() {
        try {
            out.println("salir");
            socket.close();
            sendButton.setEnabled(false);
            fileButton.setEnabled(false);
            disconnectButton.setEnabled(false);
            connectButton.setEnabled(true);
            messageArea.append("Desconectado del servidor.\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String message = inputField.getText();
        if (!message.isEmpty()) {
            out.println(message);
            inputField.setText("");
        }
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
    int returnValue = fileChooser.showOpenDialog(frame);
    if (returnValue == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        try {
            out.println("archivo:" + file.getName());
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesLeidos;
            OutputStream outputStream = socket.getOutputStream();

            while ((bytesLeidos = fis.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesLeidos);
            }
            fis.close();
            messageArea.append("Archivo enviado: " + file.getName() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    }

    class ReaderHandler implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    messageArea.append(message + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
