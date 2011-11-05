package de.tu_darmstadt.informatik.tk.tklein.ps.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.util.ByteArrayBuffer;

import pk.aamir.stompj.Connection;
import pk.aamir.stompj.ErrorHandler;
import pk.aamir.stompj.ErrorMessage;
import pk.aamir.stompj.Message;
import pk.aamir.stompj.MessageHandler;
import pk.aamir.stompj.StompJException;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class PublicSignageClient extends Activity {
	protected Connection connection = null;
	protected String cachePath = null;
	protected EditText addressField = null;
	protected EditText portField = null;
	protected EditText topicField = null;
	protected Button connectButton = null;
	protected TextView statusText = null;
	protected boolean isConnect = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		cachePath = createCache();
		
		addressField = (EditText) findViewById(R.id.addressfield);
		portField = (EditText) findViewById(R.id.portfield);
		topicField = (EditText) findViewById(R.id.topicfield);

		connectButton = (Button) findViewById(R.id.connectbutton);
		connectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String address = addressField.getText().toString();
				int port = new Integer(portField.getText().toString());
				String topic = topicField.getText().toString();
				if (!isConnect) {
					connect(address, port, topic);
					connectButton.setText("Disconnect");
					isConnect = true;
					System.out.println("Connect to " + address + ":" + port
							+ " " + topic);
				} else {
					connection.disconnect();
					System.out.println("Disconnect");
					isConnect = false;
					connectButton.setText("Connect");
					connection = null;
				}
			}
		});

		statusText = (TextView) findViewById(R.id.statustext);

	}

	public void connect(String address, int port, String topic) {
		// Stomp connection
		connection = new Connection(address, port);

		// Stomp Error handling
		connection.setErrorHandler(new ErrorHandler() {
			public void onError(ErrorMessage errorMsg) {
				System.out.println("Connection error: "+errorMsg.getMessage());
			}
		});

		try {
			connection.connect();
		} catch (StompJException e) {
			System.out.println("Connection error: "+e);
		}

		// Stomp topic subscribtion
		connection.subscribe(topic, true);

		// Stomp message handler
		connection.addMessageHandler(topic, new MessageHandler() {
			public void onMessage(Message msg) {
				String messageString = msg.getContentAsString();
				System.out.println("Received command: " + messageString);

				String messageSplit[] = messageString.split("\\s+");
				String type = messageSplit[0];
				String path = messageSplit[1];

				if (type.equals("image")) {
					openImage(path);
				} else if (type.equals("video")) {
					openVideo(path);
				} else if (type.equals("web")) {
					openWebsite(path);
				}
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		System.out.println("Application destroyed");
		connection.disconnect();
		cleanCache(cachePath);
	}

	protected void openImage(String path) {
		String fileName = "/" + getFileNameFormURL(path);
		File file = new File(cachePath + fileName);

		if (file.exists()) {
			file.delete();
			System.out.println("Deleting: " + file.getAbsolutePath());
		}

		downloadFromUrl(path, fileName);

		System.out.println("Open image: " + file.getAbsolutePath());

		Intent intent = new Intent();
		intent.setAction(android.content.Intent.ACTION_VIEW);

		intent.setDataAndType(Uri.fromFile(file), "image/*");
		
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		startActivity(intent);
	}

	protected void openVideo(String path) {
		Intent intent = new Intent();
		intent.setAction(android.content.Intent.ACTION_VIEW);
		Uri uri = Uri.parse(path);

		System.out.println("Open video: " + path);

		intent.setDataAndType(uri, "video/*");
		
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		startActivity(intent);
	}

	protected void openWebsite(String path) {
		Intent intent = new Intent(Intent.ACTION_VIEW);

		System.out.println("Open website: " + path);

		intent.setData(Uri.parse(path));
		
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		startActivity(intent);
	}

	protected void downloadFromUrl(String path, String fileName) {
		try {
			URL url = new URL(path);
			File file = new File(cachePath, fileName);
			URLConnection ucon = url.openConnection();
			InputStream is = ucon.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);
			ByteArrayBuffer baf = new ByteArrayBuffer(50);

			System.out.println("Download image: " + path);

			int current = 0;
			while ((current = bis.read()) != -1) {
				baf.append((byte) current);
			}
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(baf.toByteArray());
			
			fos.close();
			bis.close();
			is.close();
		} catch (IOException e) {
			System.out.println("Image download error: " + e);
		}
	}

	protected void cleanCache(String cachePath) {
		System.out.println("Clean Cache: "+cachePath);
		File path = new File(cachePath);
		for (File file : path.listFiles()) {
			file.delete();
		}
	}

	protected String createCache() {

		File cacheDirectory = new File(Environment
				.getExternalStorageDirectory(), "publicsignage");
		
		if (cacheDirectory.exists()) {
			cacheDirectory.delete();
		}

		cacheDirectory.mkdirs();
		
		System.out.println("Create Cache: "+cacheDirectory);
		
		return cacheDirectory.getAbsolutePath();
	}

	protected String getFileNameFormURL(String path) {
		String fileName = path.substring(path.lastIndexOf('/') + 1, path
				.length());
		return fileName;
	}
}