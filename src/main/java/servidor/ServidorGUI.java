package servidor;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ServidorGUI {
    private JFrame frame;
    private JTextArea textArea;
    private JButton startButton;
    private JButton stopButton;
    private ServerSocket serverSocket;
    private boolean running = false;
    private final List<ClientHandler> clients = new ArrayList<>();
    private static final String FILE_SAVE_DIR = "archivos_recibidos/"; // Directorio donde se guardarán los archivos

    public static void main(String[] args) {
        new ServidorGUI();
    }

    public ServidorGUI() {
        frame = new JFrame("Servidor TCP");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);

        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        startButton = new JButton("Iniciar Servidor");
        stopButton = new JButton("Detener Servidor");
        stopButton.setEnabled(false);

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.add(startButton);
        panel.add(stopButton);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(panel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    public void startServer() {
        running = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        textArea.append("Servidor iniciado...\n");

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(1234);
                while (running) {
                    Socket socket = serverSocket.accept();
                    ClientHandler clientHandler = new ClientHandler(socket);
                    clients.add(clientHandler);
                    new Thread(clientHandler).start();
                }
            } catch (IOException e) {
                textArea.append("Error en el servidor: " + e.getMessage() + "\n");
            }
        }).start();
    }

    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            for (ClientHandler client : clients) {
                client.sendMessage("Servidor detenido.");
                client.closeConnection();
            }
            textArea.append("Servidor detenido.\n");
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        } catch (IOException e) {
            textArea.append("Error al detener el servidor: " + e.getMessage() + "\n");
        }
    }

    public synchronized void broadcast(String message, ClientHandler excludeUser) {
        for (ClientHandler client : clients) {
            if (client != excludeUser) {
                client.sendMessage(message);
            }
        }
    }

    public synchronized void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void closeConnection() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                out.println("Ingresa tu nombre: ");
                clientName = in.readLine();
                broadcast(clientName + " se ha conectado.", this);
                textArea.append(clientName + " se ha conectado.\n");

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("archivo:")) {
                        String fileName = message.split(":")[1];
                        recibirArchivo(fileName);
                    } else if (message.equalsIgnoreCase("salir")) {
                        break;
                    } else {
                        textArea.append(clientName + ": " + message + "\n");
                        broadcast(clientName + ": " + message, this);
                    }
                }

                in.close();
                out.close();
                socket.close();
                textArea.append(clientName + " se ha desconectado.\n");
                broadcast(clientName + " se ha desconectado.", this);
                removeClient(this);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void recibirArchivo(String fileName) {
            try {
                // Crear el directorio si no existe
                File directorio = new File(FILE_SAVE_DIR);
                if (!directorio.exists()) {
                    directorio.mkdirs();
                }

                // Obtener la fecha y hora actual para adjuntarla al nombre del archivo
                String timeStamp = new SimpleDateFormat("MM-dd-HH-mm-ss").format(new Date());
                String nuevoNombreArchivo = fileName.split("\\.")[0] + "_" + timeStamp + "." + fileName.split("\\.")[1];

                // Crear el archivo con el nuevo nombre
                File archivo = new File(FILE_SAVE_DIR + nuevoNombreArchivo);
                FileOutputStream fos = new FileOutputStream(archivo);
                InputStream is = socket.getInputStream();

                byte[] buffer = new byte[4096];
                int bytesLeidos;
                while ((bytesLeidos = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, bytesLeidos);
                }
                fos.close();
                textArea.append("Archivo recibido: " + nuevoNombreArchivo + "\n");
            } catch (IOException e) {
                textArea.append("Error al recibir el archivo: " + e.getMessage() + "\n");
            }
        }
    }
}
