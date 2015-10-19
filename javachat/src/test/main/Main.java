package test.main;

import test.server.FileServer;
import test.server.Server;

public class Main {
	public static void main(String[] args) {
		FileServer file = FileServer.getInstance();
		Server server = Server.getInstance();
		
		file.start();
		server.runServer();
	}
}
