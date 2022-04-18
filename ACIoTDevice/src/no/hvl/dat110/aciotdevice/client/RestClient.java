package no.hvl.dat110.aciotdevice.client;

import java.io.IOException;

import com.google.gson.Gson;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RestClient {
	
	OkHttpClient client;
	Gson gson;
	
	public RestClient() {
		client = new OkHttpClient();
		gson = new Gson();
	}

	private static String logpath = "/accessdevice/log/";

	public void doPostAccessEntry(String message) {

		// TODO: implement a HTTP POST on the service to post the message
		AccessMessage accessMsg = new AccessMessage(message);
		MediaType JSON = MediaType.parse("application/json; charset=utf-8");
		RequestBody body = RequestBody.create(JSON, gson.toJson(accessMsg));
		
		Request req = new Request.Builder()
				.url("http://localhost:8080" + logpath)
				.post(body)
				.build();
		
		try (Response response = client.newCall(req).execute()) {
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private static String codepath = "/accessdevice/code";
	
	public AccessCode doGetAccessCode() {

		AccessCode code = null;
		
		// TODO: implement a HTTP GET on the service to get current access code
		Request req = new Request.Builder()
				.url("http://localhost:8080" + codepath)
				.get()
				.build();
		
		try (Response res = client.newCall(req).execute()) {
			String resString = res.body().string();
			System.out.println("\nResponse: \n" + resString);
			code = gson.fromJson(resString, AccessCode.class);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return code;
	}
}
