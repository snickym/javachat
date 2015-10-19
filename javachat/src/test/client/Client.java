package test.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Scanner;


public class Client {

	private Socket socket;
	private DataOutputStream dos;
	private DataInputStream dis;
	
	private String host;
	private int port;
	private int fport;
	
	// 0 : 로그인 안됨, 1: 로그인 중, 2 : 텍스트 전송, 3 : 파일 전송, 4 : 파일 다운로드
	private int status = 0;
	private HashMap<String, String> fileInfo;
	private String id;
	
	public Client(String host, int port, int fport){
		this.host = host;
		// 서버 포트
		this.port = port;
		// 파일서버 포트
		this.fport = fport;
		
		try {
			this.socket = new Socket(this.host, this.port);
			// 업로드 & 다운로드 파일 정보 저장
			this.fileInfo = new HashMap<String, String>();
			
			// IO 스레드 시작
			Thread writer = new Writer();
			Thread reader = new Reader();
			
			writer.start();
			reader.start();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// 모든 IO 닫음
	public void closeAll(){
		try { dis.close(); } catch (IOException e) {}
		try { dos.close(); } catch (IOException e) {}
		try { socket.close(); } catch (IOException e) {}
	}

	// OutputStream 
	class Writer extends Thread {
		Writer() {
			try {
				dos = new DataOutputStream(socket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			Scanner scn = new Scanner(System.in);
			while(dos != null){
				try {
					// 0 : 로그인 안됨, 1: 로그인 중, 2 : 텍스트 전송, 3 : 파일 전송, 4 : 파일 다운로드
					if(status == 0){
						System.out.print("[System] 사용할 아이디를 입력하세요 : ");
					}
					
					String msg = scn.nextLine();
					
					if(status == 0){
						// 입력 받은 id가 중복인지 확인하기 위해 서버에 메세지 보내고 대기
						System.out.println("[System] 중복여부를 확인하고 있습니다....");
						dos.writeUTF("[init]|" + msg);
						dos.flush();
						id = msg;
						status = 1;
					} else if(status == 2) {
						// 로그인 완료
						// 파일 전송모드로 변경, '[file] 파일경로'의 형태이며 반드시 한칸 띄워야 함
						if(msg.startsWith("[file]")){
							status = 3;
							fileInfo.put("orgName", msg.split(" ")[1].trim());
							System.out.print("[System] 파일을 전송하시겠습니까?(y/n) : ");
						} else {
							// [file]이 붙지 않으면 그냥 텍스트 전송
							dos.writeUTF("[text]|" + msg);
						}
						dos.flush();
					} else if(status == 3) {
						// 파일을 전송한다면
						if(msg.equals("y")){
							// FileUploader 클래스에 경로가 포함된 파일이름을 넘겨주고 스레드 시작
							FileUploader fileUploader = new FileUploader(fileInfo.get("orgName"));
							fileUploader.start();
						}
						// 텍스트 전송모드로 변경
						// 파일 정보는 초기화
						status = 2;
						fileInfo.clear();
					} else if(status == 4) {
						// 파일을 다운로드한다면
						if(msg.equals("y")){
							// FileDownloader 클래스에 파일정보(이름, uuid, 발신자) 넘겨주고 다운로드 시작
							FileDownloader downloader = new FileDownloader(fileInfo, true);
						} else {
							// FileDownloader 클래스에 파일정보(이름, uuid, 발신자) 넘겨주고 다운로드 하지 않음을 알림
							FileDownloader downloader = new FileDownloader(fileInfo, false);
						}
						// 텍스트 전송모드로 변경
						// 파일 정보는 초기화
						status = 2;
						fileInfo.clear();
					}
				} catch(SocketException e){      
					e.printStackTrace();
	                System.out.println("[System] 서버와 연결이 끊어졌습니다.");
	                closeAll();
	                // break하지 않으면 SocketException가 무한으로 뜸
	                break;
	            } catch (Exception e) {
					closeAll();
				}
			} 
		}
		
	}
	
	// InputStream
	class Reader extends Thread{
		
		Reader() {
			try {
				dis = new DataInputStream(socket.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			while(dis != null){
				try {
					String msg = dis.readUTF();
					if(status == 1){
						// 로그인 대기 중인데 중복이 아니라면
						if(msg.split("\\|")[1].equals("y")){
							// 텍스트 전송모드로 변경
							status = 2;
						} else {
							// 로그인 안 된 상태로 변경
							status = 0;
							id = "";
							System.out.print("[System] 사용할 아이디를 입력하세요 : ");
						}
					} else if(status > 1) {
						if(msg.startsWith("[download]")){
							// 다운로드할 것인지 요청이 들어오면 다운로드 모드로 변경
							status = 4;
							fileInfo.put("sender", msg.split("\\|")[1]);
							fileInfo.put("orgName", msg.split("\\|")[2]);
							fileInfo.put("uuid", msg.split("\\|")[3]);
							// 다운로드 파일정보 및 발신자 안내
							System.out.print("[System] " + fileInfo.get("sender") + "가 보낸 " + fileInfo.get("orgName") +"파일을 받으시겠습니까?(y/n) : ");
							
						} else if(msg.startsWith("[exit]")){
							// 채팅 유저 퇴장 안내
							System.out.println(msg.split("\\|")[1] + "님이 퇴장하셨습니다." );
						} else if(msg.startsWith("[enter]")){
							// 채팅 유저 입장 안내
							System.out.println(msg.split("\\|")[1] + "님이 입장하셨습니다.");
						} else if(msg.startsWith("[text]")){
							// 일반 채팅 텍스트
							System.out.println(msg.split("\\|")[1] + " : " + msg.split("\\|")[2]);
						}
					} 
				}catch(SocketException e){   
	                System.out.println("[System] 서버와 연결이 끊어졌습니다.");
	                closeAll();
	                break;
	            } catch (Exception e) {
					e.printStackTrace();
					closeAll();
				}
			}
		}
	}

	class FileUploader extends Thread{
		
		private Socket fSocket;
		private DataOutputStream fDos;
		private FileInputStream fis;
			
		FileUploader(String filePath) throws Exception{
			// 파일경로 + 파일이름으로 File 객체 생성
			File file = new File(filePath);
			// 파일서버포트로 소켓연결
			this.fSocket = new Socket(host, fport);
			this.fDos = new DataOutputStream(fSocket.getOutputStream());
			this.fis = new FileInputStream(file);
			// 파일서버에 발신자와 파일이름 전송
			fDos.writeUTF("[upload]|" + id + "|" + file.getName());
			fDos.flush();
		}
		
		@Override
		public void run() {
			try {	// 파일전송 시작
					byte[] buf = new byte[1024];
					int read = -1;
					// (read = fis.read(buf, 0, buf.length)) != -1로 하는 경우가 있는데 EOF Exception이 발생하기도 함
					while((read = fis.read(buf, 0, buf.length)) > 0){
						fDos.write(buf, 0, read);
					}
					fDos.flush();
					System.out.println("[System] 업로드를 완료했습니다.");
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try { if(fis != null) {fis.close(); }} catch (IOException e) {}
				try { if(fDos != null) {fDos.close(); }} catch (IOException e) {}
				try { if(fSocket != null) {fSocket.close(); }} catch (IOException e) {}
			}
		}
	}
	class FileDownloader extends Thread{
		
		private Socket fSocket;
		private DataInputStream fDis;
		private DataOutputStream fDos;
		private FileOutputStream fos;
		
		private String FILE_PATH = "/Users/SSM/Downloads/";
		
		FileDownloader(HashMap<String, String> fileInfo, boolean isDownload) {
			try {
				this.fSocket = new Socket(host, fport);
				this.fDis = new DataInputStream(fSocket.getInputStream());
				this.fDos = new DataOutputStream(fSocket.getOutputStream());
				
				// 받는다면 
				if(isDownload){
					yesDownload();
					// 스레드 실행
					this.start();
				} else {
					// 받지 않는다면
					noDownload();
				}
			}catch(SocketException e){   
                System.out.println("[System] 서버와 연결이 끊어졌습니다.");
                closeAll();
            } catch (Exception e) {
				e.printStackTrace();
				closeAll();
			}
		}
		
		// 서버에 저장되어있는 파일을 요청함
		public void yesDownload() throws Exception {
			// 서버에서 받은 파일이름으로 File객체 생성
			File file = new File(FILE_PATH + fileInfo.get("orgName"));
			fos = new FileOutputStream(file);
			fDos.writeUTF("[download]|" + id + "|" + fileInfo.get("uuid"));
			fDos.flush();
		}

		// 파일을 받지 않음
		public void noDownload() throws Exception {
			try {
				fDos.writeUTF("[download]|" + id + "|" + fileInfo.get("uuid") + "|n");
				fDos.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			try {	// 파일 다운로드 
					byte[] buf = new byte[1024];
					int read = 0;
					while((read = fDis.read(buf, 0, buf.length)) > 0){
						fos.write(buf, 0, read);
				}
				System.out.println("[System] 다운로드를 완료했습니다.");
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				closeAll();
			}
		}
		
		public void closeAll(){
			try { if(fos != null) {fos.close(); }} catch (IOException e) {}
			try { if(fDis != null) {fDis.close();}} catch (IOException e) {}
			try { if(fDos != null) {fDos.close(); }} catch (IOException e) {}
			try { if(fSocket != null) {fSocket.close(); }} catch (IOException e) {}
		}
	}
	
	public static void main(String[] args) {
		Client client = new Client("127.0.0.1", 8083, 8082);
	}
}
