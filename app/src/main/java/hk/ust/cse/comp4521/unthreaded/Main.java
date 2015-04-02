package hk.ust.cse.comp4521.unthreaded;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.Result;


public class Main extends Activity implements View.OnClickListener {
    private ProgressBar progressBar;
    private TextView statusText;
    private int completed;
    private int silly = 3;
    //Zhou Xu Tong is Silly

    private static final String serverurl = "http://175.159.123.22/task_manager/v1/register";

    private class WorkerTask extends AsyncTask<Object, String, Boolean> {
        // Initialize the progress bar and the status TextView
        // Initialize the progress bar and the status TextView
        @Override
        protected void onPreExecute() {
            completed = 0;
            // This will result in a call to onProgressUpdate()
            publishProgress();
        }
        @Override
        // This method updates the main UI, refreshing the progress bar and TextView.
        // Finish this method by yourself.
        protected void onProgressUpdate(String... values) {
            progressBar.setProgress(completed);
            statusText.setText(String.format("Completed %d", completed));
        }
        // Do the main computation in the background and update the UI using publishProgress()
        // Finish this method by yourself.
        @Override
        protected Boolean doInBackground(Object... params) {

            int l;
            for (int i = 0; i< 100; ++i) {
                for (int j = 0; j < 5000; ++j) {
                    for (int k = 0; k < 5000; ++k) {
                        l = i * j * k;
                        if (isCancelled()) break;
                    }
                    if (isCancelled()) break;
                }
                completed += 1;
                publishProgress();
                if (isCancelled()) break;
                if (completed == 100) break;
            }
            return null;
        }

        protected void onPostExecute(Boolean result){
            statusText.setText(String.format("Completed!!!!!"));
        }
    }

    WorkerTask worker;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        //handler = new Handler();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
// Get handles for the progress bar and status text TextView
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        statusText = (TextView) findViewById(R.id.status_text);
// Set the maximum value that the progress bar will display
        progressBar.setMax(100);
// Declare the listeners for the two buttons
        Button startButton = (Button) findViewById(R.id.start_button);
        startButton.setOnClickListener(this);
        Button resetButton = (Button) findViewById(R.id.reset_button);
        resetButton.setOnClickListener(this);

        if (isOnline()) {
            new AsyncHttpPost().execute(serverurl);
        }else{
            Toast.makeText(this, getString(R.string.notOnline), Toast.LENGTH_LONG).show();
        }
    }

    private boolean isOnline() {

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        else {
            return false;
        }
    }

    public void onClick(View source) {
// Start button is clicked
        if (source.getId() == R.id.start_button) {
            if(worker != null){
                worker.cancel(false);
            }
            worker = new WorkerTask();
            worker.execute();
            //Thread workerthread = new Thread(worker);
            //workerthread.start();
        }
// Reset button is clicked
        else if (source.getId() == R.id.reset_button) {
            worker.cancel(false);
            progressBar.setProgress(0);
            statusText.setText(String.format("Click the button"));
            completed = 0;
        }
    }

    public class AsyncHttpPost extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... params) {

            try {
                NameValuePair pair1 = new BasicNameValuePair("name", "Peng Rui");
                NameValuePair pair2 = new BasicNameValuePair("email", "123456789@qq.com");
                NameValuePair pair3 = new BasicNameValuePair("password", "ddd");

                List<NameValuePair> pairList = new ArrayList<NameValuePair>();
                pairList.add(pair1);
                pairList.add(pair2);
                pairList.add(pair3);

                HttpEntity requestHttpEntity = new UrlEncodedFormEntity(pairList);
                // URL使用基本URL即可，其中不需要加参数
                HttpPost httpPost = new HttpPost(serverurl);
                // 将请求体内容加入请求中
                httpPost.setEntity(requestHttpEntity);
                // 需要客户端对象来发送请求
                HttpClient httpClient = new DefaultHttpClient();
                // 发送请求
                HttpResponse response = httpClient.execute(httpPost);

                StatusLine a = response.getStatusLine();
                Log.i("IMPORTANT", "POST request: Status code = " + a.getStatusCode());



            } catch (Exception e) {
                e.printStackTrace();
            }



            return 0;
        }

    }

}