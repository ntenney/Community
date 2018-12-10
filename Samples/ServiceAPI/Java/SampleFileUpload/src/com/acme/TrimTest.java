
package com.acme;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.auth.NTLMScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.acme.dto.Record;
import com.acme.dto.RecordRef;
import com.acme.dto.RecordTypeRef;
import com.acme.dto.RecordsResponse;
import com.acme.dto.TrimStringProperty;
import com.acme.dto.UploadFileResponse;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;

import java.io.File;
import java.io.IOException;
//import net.servicestack.client.JsonServiceClient;
//import net.servicestack.client.Utils;
//import net.servicestack.client.WebServiceException;

import javax.net.ssl.SSLContext;

import static org.apache.http.conn.ssl.SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;

class WebServiceException extends RuntimeException{
	private int responseCode;
	private String statusLine;
	private String responseString;

	public WebServiceException(int responseCode, String statusLine, String responseString)
	{
		this.responseCode = responseCode;
		this.statusLine = statusLine;
		this.responseString = responseString;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	public String getStatusLine() {
		return statusLine;
	}

	public void setStatusLine(String statusLine) {
		this.statusLine = statusLine;
	}

	public String getResponseString() {
		return responseString;
	}

	public void setResponseString(String responseString) {
		this.responseString = responseString;
	}


}

public class TrimTest {

	private static UsernamePasswordCredentials creds = new UsernamePasswordCredentials("PNL\\eustrim", "suckGo12");
//	private static String baseUrl = "https://ermsdev.pnl.gov:443/HPECMServiceAPI/";
	private static String baseUrl = "https://ermsdev.pnl.gov/HPECMServiceAPI/";
	private static long CONTAINER_URI = 1838257L;
	private static long RECORD_TYPE_URI = 134L;
	
	private static ArrayList<String> makePropertyList(String... propertyNames) {
		ArrayList<String> properties = new ArrayList<String>();
		
		if (propertyNames.length > 0) {
			for (String propertyName : propertyNames) {
				properties.add(propertyName);
			}
		} else {
			properties.add("RecordTitle");
			properties.add("RecordNumber");
		}
		return properties;
	}

	private static RuntimeException createException(int code, HttpResponse response)  {

		HttpEntity responseEntity = response.getEntity();
		String responseString;

		WebServiceException webEx = null;

		try {
			responseString = EntityUtils.toString(responseEntity, "UTF-8");

			webEx = new WebServiceException(code, response.getStatusLine().getReasonPhrase(), responseString);

			Gson gson = new Gson();

			JsonElement element = gson.fromJson(responseString, JsonElement.class);
			if(element != null) {
				JsonObject jsonObj = element.getAsJsonObject();

				for (Map.Entry<String,JsonElement> jsonElementEntry : jsonObj.entrySet()) {
					if(jsonElementEntry.getKey().toLowerCase().equals("responsestatus")) {
//						webEx.setResponseStatus(Utils.createResponseStatus(jsonObj.get(jsonElementEntry.getKey())));
						break;
					}
				}

			}
			return webEx;
		} catch ( JsonSyntaxException | ParseException | IOException e) {
			if (webEx != null)
				return webEx;
			return new RuntimeException(e);
		}

	}

	private static String submitRecord(String record) throws ClientProtocolException, IOException, AuthenticationException, KeyManagementException, NoSuchAlgorithmException{
		String uploadUrl = baseUrl + "Record"; // e.g. full URL http://myserver/ServiceAPI/Uploadfile
		SSLContext sslContext = SSLContexts.custom()
				.useTLS()
				.build();

		SSLConnectionSocketFactory f = new SSLConnectionSocketFactory(
				sslContext,
				new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"},
				new String[]{"TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384"},
				BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);

		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(AuthScope.ANY, new NTCredentials("eustrim","suckGo12", "we27967.pnl.gov","pnl"));

		HttpClient client = HttpClientBuilder.create().setSSLSocketFactory(f).setDefaultCredentialsProvider(credsProvider).build();

		HttpPost post = new HttpPost(uploadUrl);

		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
//		Authenticator.setDefault(new MyAuthenticator("pnl\\eustrim", "suckGo12"));

//		post.addHeader(new BasicScheme().authenticate(creds, post, null));
//		post.addHeader(new NTLMScheme().authenticate(creds,post,null));


		post.setHeader("Accept", "application/json");
		post.setHeader("Content-type", "application/json");

		post.setEntity(new StringEntity(record));
		HttpResponse response = client.execute(post);

		int code = response.getStatusLine().getStatusCode();
		if (code != 200) {

			throw createException(code, response);
		}

		HttpEntity responseEntity = response.getEntity();
		String responseString = EntityUtils.toString(responseEntity, "UTF-8");

		System.out.println(responseString.toString());
		return responseString;
	}
	
	
//	private static String uploadFile(String filePath, JsonServiceClient sapiClient) throws ClientProtocolException, IOException, AuthenticationException, KeyManagementException, NoSuchAlgorithmException {
    private static String uploadFile(String filePath) throws ClientProtocolException, IOException, AuthenticationException, KeyManagementException, NoSuchAlgorithmException {

//		String uploadUrl = baseUrl + "resource/UploadFile"; // e.g. full URL http://myserver/ServiceAPI/Uploadfile
		String uploadUrl = baseUrl + "UploadFile"; // e.g. full URL http://myserver/ServiceAPI/Uploadfile
		SSLContext sslContext = SSLContexts.custom()
				.useTLS()
				.build();
		
		SSLConnectionSocketFactory f = new SSLConnectionSocketFactory(
				sslContext,
				new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"},
				new String[]{"TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384"},
				BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
		
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(AuthScope.ANY, new NTCredentials("eustrim","suckGo12", "we27967.pnl.gov","pnl"));
		
		HttpClient client = HttpClientBuilder.create().setSSLSocketFactory(f).setDefaultCredentialsProvider(credsProvider).build();
		
		HttpPost post = new HttpPost(uploadUrl);
		
		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
//		Authenticator.setDefault(new MyAuthenticator("pnl\\eustrim", "suckGo12"));

//		post.addHeader(new BasicScheme().authenticate(creds, post, null));
//		post.addHeader(new NTLMScheme().authenticate(creds,post,null));
  
		
		post.setHeader("Accept", "application/json");
		
		File file = new File(filePath);
		
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		builder.addBinaryBody("Files", file, ContentType.DEFAULT_BINARY, file.getName());
		
		HttpEntity entity = builder.build();
		post.setEntity(entity);

		HttpResponse response = client.execute(post);
		
		int code = response.getStatusLine().getStatusCode();
		
		if (code != 200) {
			
            throw createException(code, response);
		}
		
		HttpEntity responseEntity = response.getEntity();
		String responseString = EntityUtils.toString(responseEntity, "UTF-8");

		//UploadFileResponse uploadedFile = (UploadFileResponse) sapiClient.fromJson(responseString, UploadFileResponse.class);
		Gson gson = new Gson();
		UploadFileResponse uploadedFile = gson.fromJson(responseString, UploadFileResponse.class);

		return uploadedFile.FilePath;


	}
	
	public static void main(String[] args) throws AuthenticationException, ClientProtocolException, IOException {
		System.setProperty("https.protocols", "TLSv1.2");
//		JsonServiceClient sapiClient = new JsonServiceClient(baseUrl);
		
//		sapiClient.setCredentials(creds.getUserName(), creds.getPassword());
//		sapiClient.setAlwaysSendBasicAuthHeaders(true);


		Record request = new Record();

		RecordTypeRef recordType = new RecordTypeRef();
		recordType.setUri(RECORD_TYPE_URI);
		request.setRecordType(recordType);
		RecordRef container = new RecordRef();
		container.setUri(CONTAINER_URI);
		request.setContainer(container);

		try {
//			String filePath = uploadFile("/Users/natet/tmp/s41467_018_04862_w.pdf", sapiClient);
			String filePath = uploadFile("/Users/natet/tmp/s41467_018_04862_w.pdf");

			TrimStringProperty filePathProperty = new TrimStringProperty();
			filePathProperty.setValue(filePath);

			request.setFilePath(filePathProperty);

			request.setTitle(new TrimStringProperty().setValue("This is a test"));


			request.Properties = makePropertyList("RecordTitle");
//            sapiClient.setAlwaysSendBasicAuthHeaders(true);

//			String jsonDoc = sapiClient.toJson(request);
			Gson gson = new Gson();
            String jsonDoc = gson.toJson(request);

			String responseString = submitRecord(jsonDoc);

//			RecordsResponse response = sapiClient.post(request);
			RecordsResponse response = gson.fromJson(responseString, RecordsResponse.class);

			for (dto.Record record : response.Results) {
				System.out.println(record.getTitle().Value);
			}

		}
		catch(WebServiceException ex) {
//			System.out.println(ex.getStatusCode());
            System.out.println(ex.getResponseCode());
//			System.out.println(ex.getErrorMessage());
			System.out.println(ex.getStatusLine());
			ex.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
		
		
	}

}

class MyAuthenticator extends Authenticator {
	private String httpUsername;
	private String httpPassword;

	public MyAuthenticator(String httpUsername, String httpPassword) {
		this.httpUsername = httpUsername;
		this.httpPassword = httpPassword;
	}

	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		System.out.println("Scheme:" + getRequestingScheme());
		return new PasswordAuthentication(httpUsername, httpPassword.toCharArray());
	}
}
