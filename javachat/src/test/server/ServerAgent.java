package test.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class ServerAgent extends Thread {

	private Socket socket;
	private DataInputStream dis;
	private DataOutputStream dos; 
	
	private String userId;
	
	// Serversocket으로 들어오는 접속을 감지하면 ServerAgent 객체를 생성
	// 1사용자 1ServerAgent
	public ServerAgent(Socket socket){
		try {
			this.socket = socket;
			this.dis = new DataInputStream(socket.getInputStream());
			this.dos = new DataOutputStream(socket.getOutputStream());
		} catch (Exception e) {
			closeAll();
		}
	}

	public void closeAll(){
		try { if(dos != null) {dos.close();} } catch (IOException e) {}
		try { if(dis != null) {dis.close();} } catch (IOException e) {}
		try { if(socket != null) {socket.close();} } catch (IOException e) {}
	}
	
	@Override
	public void run() {
		try {
			// 해당 사용자로부터의 응답을 받음
			while(dis != null){
				read();
			}
		} catch (Exception e) {
		} finally {
			closeAll();
			System.out.println("접속해제 : " + socket.toString());
			Server.getInstance().exitUser(this);
		}
	}

	public void read() throws Exception{
		String msg = null;
		msg = dis.readUTF();
		if(msg.startsWith("[init]")){
			// 로그인 시도, id가 정해진 적이 없을 경우 Server의 사용자 리스트에서 중복된 아이디를 사용하는 사용자가 있는지 확인
			// 있으면 n, 없으면 y를 보낸 후 다른 사용자들에게 새로운 사용자가 들어왔음을 알림
			if(userId == null){
				String id = msg.split("\\|")[1];
				if(Server.getInstance().existId(id)){
					write("[init]|n");
				} else {
					userId = id;
					write("[init]|y");
					Server.getInstance().enterUser(userId);
				}
			} 
		} else if(msg.startsWith("[text]")){
			// 텍스트 전송이 들어오면 모든 사용자에게 사용자의 메세지를 전송
			Server.getInstance().broadcast(msg.split("\\|")[1], userId);
		}
	}
	
	// 사용자에게 메세지를 전달, 주로 Server에서 호출
	public void write(String msg){
		try {
			dos.writeUTF(msg);
			dos.flush();
		} catch (Exception e) {
			closeAll();
		}
	}

	// 사용자의 아이디를 반환
	public String getUserId() {
		return userId;
	}
	
}
