package test.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
	// Singleton 패턴
	private volatile static Server instance;
	public static Server getInstance(){
		synchronized (Server.class) {
			if(instance == null){
				instance = new Server();
			}
		}
		return instance;
	}
	
	private ServerSocket server;
	private ArrayList<ServerAgent> clients;
	public Server(){
		try {
			// 8083번 포트에 서버소켓 생성
			this.server = new ServerSocket(8083);
			// ServerAgent list 생성
			this.clients = new ArrayList<ServerAgent>();
			System.out.println("Server On");
		} catch (Exception e) {
			System.out.println("[Error555] : " + e);
		}
	}
	
	public void runServer(){
		while(true){
			try {
				// 클라이언트로부터 소켓이 생성되는 것을 대기
				Socket socket = server.accept();
				System.out.println("접속 : " + socket.toString());
				// ServerAgent 생성 뒤, clients에 추가하고 agent 스레드를 실행
				ServerAgent agent = new ServerAgent(socket);
				clients.add(agent);
				agent.start();
			} catch (Exception e) {
				System.out.println("[Error666] : " + e);
			}
		}
	}
	
	// client 리스트에서 동일한 아이디의 사용자가 있는지 확인
	public boolean existId(String userId){
		for(int i = 0, len = clients.size(); i<len; i++){
			if(userId.equals(clients.get(i).getUserId())){
				return true;
			}
		}
		return false;
	}
	
	// 해당 아이디를 사용하는 사용자의 ServerAgent를 반환
	public ServerAgent getUser(String userId){
		ServerAgent agent = null;
		for(ServerAgent client : clients){
			if(client.getUserId().equals(userId)){
				agent = client;
				break;
			}
		}
		return agent;
	}
	
	// 사용자 아이디 리스트 반환
	public ArrayList<String> getUserList(){
		ArrayList<String> uList = new ArrayList<String>();
		for(ServerAgent client : clients){
			uList.add(client.getUserId());
		}
		return uList;
	}
	
	// 모든 사용자에게 텍스트 채팅 전달
	public void broadcast(String msg, String userId){
		for(ServerAgent client : clients){
			client.write("[text]|" + userId + "|" + msg);
		}
	}
	public void broadcastFile(String userId, String orgName, String uuid){
		for(ServerAgent client : clients){
			if(!client.getUserId().equals(userId)){
				client.write("[download]|" + userId + "|" + orgName + "|" + uuid);
			}
		}
	}
	
	// 모든 사용자에게 새로운 사용자 입장을 알림
	public void enterUser(String user){
		for(ServerAgent client : clients){
			client.write("[enter]|" + user );
		}
	}
	
	// 모든 사용자에게 나간 사용자 퇴장을 알리고 사용자 리스트, 파일전송 리스트에서 제거
	public void exitUser(ServerAgent user){
		String exitedUser = user.getUserId();
		// 파일 리스트 내 해당 사용자 삭제
		FileServer.getInstance().removeExitedUser(exitedUser);
		// 사용자가 나갔음을 전달
		for(int i = 0; i < clients.size(); i++){
			if(!clients.get(i).equals(exitedUser)){
				clients.get(i).write("[exit]|" + exitedUser );
			}
		}
		// 해당 사용자 리스트에서 삭제
		clients.remove(user);
	}
	
}
