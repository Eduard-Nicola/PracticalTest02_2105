package ro.pub.cs.systems.eim.practicaltest02.network;


import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import ro.pub.cs.systems.eim.practicaltest02.general.Constants;
import ro.pub.cs.systems.eim.practicaltest02.general.Utilities;
import ro.pub.cs.systems.eim.practicaltest02.model.Alarm;

public class CommunicationThread extends Thread {

    private ServerThread serverThread;
    private Socket socket;
    private String clientIp;

    public CommunicationThread(ServerThread serverThread, Socket socket, String clientIp) {
        this.serverThread = serverThread;
        this.socket = socket;
        this.clientIp = clientIp;
    }

    @Override
    public void run() {
        if (socket == null) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] Socket is null!");
            return;
        }
        try {
            BufferedReader bufferedReader = Utilities.getReader(socket);
            PrintWriter printWriter = Utilities.getWriter(socket);
            if (bufferedReader == null || printWriter == null) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Buffered Reader / Print Writer are null!");
                return;
            }
            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Waiting for parameters from client (IP)");
            String command = bufferedReader.readLine();
            if (command == null || command.isEmpty()) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error receiving parameters from client (city / information type!");
                return;
            }

            // Interpret commands
            if (command.contains("reset")) {
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Received reset command");
                this.serverThread.insertData(this.clientIp, null);
                printWriter.println("Removed");
                printWriter.flush();
            } else if (command.contains("set")) {
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Received set command");
                String[] tokens = command.split(",");
                String hour = tokens[1];
                String minute = tokens[2];
                Alarm alarm = new Alarm(hour, minute);
                this.serverThread.insertData(this.clientIp, alarm);
                printWriter.println("Added");
                printWriter.flush();
            } else if (command.contains("poll")) {
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Received poll command");
                Socket hourSocket = null;
                try {
                    hourSocket = new Socket(Constants.SERVICE_ADDRESS, Constants.SERVICE_PORT);
                    BufferedReader reader = Utilities.getReader(hourSocket);

                    Alarm clientAlarm = this.serverThread.getData(this.clientIp);

                    if (clientAlarm == null) {
                        printWriter.println("none");
                        printWriter.flush();
                        socket.close();
                        return;
                    }

                    // Find alarm active in cache and skip request
                    if (this.serverThread.getAlarm(this.clientIp) != null) {
                        if (this.serverThread.getAlarm(this.clientIp)) {
                            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Alarm activated, using cache!");
                            printWriter.println("active");
                            printWriter.flush();
                            socket.close();
                            return;
                        }
                    }

                    String line;
                    line = reader.readLine();
                    line = reader.readLine();
                    String[] time_tokens = {};
                    if(!line.isEmpty()) {
                        String[] tokens = line.split(" ");

                        String time = tokens[2];
                        time_tokens = time.split(":");
                    }

                    Integer clientHour = Integer.parseInt(clientAlarm.getHour());
                    Integer clientMinute = Integer.parseInt(clientAlarm.getMinute());
                    Integer utcHour = Integer.parseInt(time_tokens[0]);
                    Integer utcMinute = Integer.parseInt(time_tokens[1]);
                    if (clientHour > utcHour || clientMinute > utcMinute) {
                        this.serverThread.insertAlarm(this.clientIp, false);
                        printWriter.println("inactive");
                        printWriter.flush();
                    } else if (clientHour < utcHour || clientMinute < utcMinute) {
                        this.serverThread.insertAlarm(this.clientIp, true);
                        printWriter.println("active");
                        printWriter.flush();
                    }

                    socket.close();
                } catch (Exception exception) {
                    Log.d(Constants.TAG, exception.getMessage());
                    if (Constants.DEBUG) {
                        exception.printStackTrace();
                    }
                }
            }
        } catch (IOException ioException) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
            if (Constants.DEBUG) {
                ioException.printStackTrace();
            }
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ioException) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
                    if (Constants.DEBUG) {
                        ioException.printStackTrace();
                    }
                }
            }
        }
    }

}
