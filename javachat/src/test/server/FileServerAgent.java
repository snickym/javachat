package test.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class FileServerAgent extends Thread {
	
	private String FILE_SAVED_PATH = "/Users/SSM/Downloads/upload/";
	
	private Socket socket;
	private DataInputStream fDis;
	private DataOutputStream fDos;
	private FileInputStream fis;
	private FileOutputStream fos;
	
	// Serversocket으로 들어오는 접속을 감지하면 ServerAgent 객체를 생성
	// 1사용자 1FileServerAgent
	public FileServerAgent(Socket socket){
		try {
			this.socket = socket;
			this.fDis = new DataInputStream(socket.getInputStream());
		} catch (Exception e) {
			closeAll();
			System.out.println("파일서버 접속해제 : " + socket.toString());
			FileServer.getInstance().exitUser(this);
		}
	}

	@Override
	public void run() {
		try {
			// 들어온 메세지 upload/download에 따라 실행 메소드가 다름 
			String msg = fDis.readUTF();
			if(msg.startsWith("[upload]")){
				upload(msg.split("\\|")[2], msg.split("\\|")[1]);
			} else if(msg.startsWith("[download]")){
				if(msg.endsWith("n")){
					// 해당 파일의 사용자 리스트에서 사용자 삭제
					FileServer.getInstance().checkReceivedFile(msg.split("\\|")[2], msg.split("\\|")[1]);
				} else {
					download(msg.split("\\|")[2], msg.split("\\|")[1]);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeAll();
		}
	}
	
	public void upload(String orgfName, String userId) throws Exception{
		// 동일한 파일명을 가진 경우 잘못된 전송을 막기 위해 uuid를 사용
		// 파일을 다운로드하는 경우에는 uuid, userId, file name 세가지를 받아서 
		// file name의 파일을 생성하고 발신자 userid가 업로드한 uuid의 파일 받음
		String uuid = UUID.randomUUID().toString();
		
		// uuid로 된 파일저장
		File file = new File(FILE_SAVED_PATH + uuid);
		fos = new FileOutputStream(file);
		byte[] buf = new byte[1024];
		int read = 0;
		while((read = fDis.read(buf, 0, buf.length)) > 0){
			fos.write(buf, 0, read);
		}
		// 파일 정보를 기록
		HashMap<String, Object> info = new HashMap<String, Object>();
		info.put("sender", userId);
		info.put("orgfName", orgfName);
		info.put("uufName", uuid);
		
		FileServer.getInstance().regFile(info);
		// 서버에 파일이 새로 올라왔음을 알림
		Server.getInstance().broadcastFile(userId, orgfName, uuid);
	}
	
	public void download(String uufName, String userId) throws Exception{
		
		fDos = new DataOutputStream(socket.getOutputStream());
		// 업로드 된 파일 객체 생성
		File file = new File(FILE_SAVED_PATH + uufName);
		fis = new FileInputStream(file);
		
		// 파일 전송
		byte[] buf = new byte[1024];
		int read = 0;
		while((read = fis.read(buf, 0, buf.length)) > 0){
			fDos.write(buf, 0, read);
		}
		fDos.flush();
		try { if(fis != null) {fis.close(); }} catch (IOException e) {}
		
		// 해당 파일의 사용자 리스트에서 사용자 삭제
		FileServer.getInstance().checkReceivedFile(uufName, userId);
	}
	
	public void closeAll(){
		try { if(fis != null) {fis.close(); }} catch (IOException e) {}
		try { if(fos != null) {fos.close(); }} catch (IOException e) {}
		try { if(fDis != null) {fDis.close();}} catch (IOException e) {}
		try { if(fDos != null) {fDos.close(); }} catch (IOException e) {}
		try { if(socket != null) {socket.close(); }} catch (IOException e) {}
	}
}
