package test.server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;


public class FileServer extends Thread {
	// Singleton 패턴
	private volatile static FileServer instance;
	public static FileServer getInstance(){
		synchronized (FileServer.class) {
			if(instance == null){
				instance = new FileServer();
			}
		}
		return instance;
	}

	private ServerSocket server;
	private ArrayList<FileServerAgent> clients;
	private ArrayList<HashMap<String, Object>> fileList;
	private String FILE_SAVED_PATH = "/Users/SSM/Downloads/upload/";
	
	public FileServer(){
		try {
			// 8082번 포트에 서버소켓 생성
			this.server = new ServerSocket(8082);
			// FileAgent 리스트 생성
			this.clients = new ArrayList<FileServerAgent>();
			// 파일리스트 생성
			this.fileList = new ArrayList<HashMap<String, Object>>();
			System.out.println("File Server On");
		} catch (Exception e) {
			System.out.println("[Error222] : " + e);
			try { server.close(); } catch (IOException e1) {}
		}
	}
	
	@Override
	public void run() {
		runServer();
	}

	public void runServer(){
		while(true){
			try {
				// 클라이언트로부터 소켓이 생성되는 것을 대기
				Socket socket = server.accept();
				System.out.println("접속 : " + socket.toString());
				// FileServerAgent 생성 뒤, clients에 추가하고 agent 스레드를 실행
				FileServerAgent agent = new FileServerAgent(socket);
				clients.add(agent);
				agent.start();
			} catch (Exception e) {
				System.out.println("[Error333] : " + e);
			}
		}
	}
	
	// 파일 전송이 끝난 사용자를 리스트에서 삭제
	public void exitUser(FileServerAgent user){
		clients.remove(user);
	}
	
	// 파일이 업로드 되는 경우, 파일을 리스트에 추가
	public void regFile(HashMap<String, Object> info){
		ArrayList<String> uList = Server.getInstance().getUserList();
		uList.remove(info.get("sender"));
		info.put("receivers", uList);
		fileList.add(info);
	}
	
	// 사용자가 나간 경우, 해당 받지 않은 파일이 있는 경우, 리스트에서 제외 시킴
	public void removeExitedUser(String userId){
		for(HashMap<String, Object> file : fileList){
			ArrayList<String> receivers = (ArrayList<String>) file.get("receivers");
			if(receivers.contains(userId)){
				receivers.remove(userId);
			}
		}
	}
	
	// 사용자가 파일 다운로드를 완료한 경우, 해당 파일의 수신자 리스트에서 삭제
	public void checkReceivedFile(String uufName, String userId){
		
		for(int i = 0; i < fileList.size(); i++){
			if(fileList.get(i).get("uufName").equals(uufName)){
				((ArrayList<String>) fileList.get(i).get("receivers")).remove(userId);
			}
		}
		
		for(int i = 0; i < fileList.size(); i++){
			// 모든 사용자가 다운로드를 완료했다면 파일리스트에서 삭제
			if(((ArrayList<String>) fileList.get(i).get("receivers")).size() == 0){
				File file = new File(FILE_SAVED_PATH + (String) fileList.get(i).get("uufName"));
				if(file.isFile() && file.exists()){
					file.delete();
					fileList.remove(fileList.get(i));
				}
			}
		}
	}
}
