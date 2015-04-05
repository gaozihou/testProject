package hk.ust.cse.comp4521.unthreaded;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.Result;

public class Main extends Activity implements View.OnClickListener {

    private ProgressBar progressBar;
    private TextView statusText;
    private int completed;
    private WorkerTask worker;
    private TextView showMessage;
    private ImageView showImage;

    private String DATABASE_PATH = "/sdcard/Unthreaded";
    private String FILE_PATH = DATABASE_PATH + "/firstFile.txt";

    private static final String serverurl = "http://gaozihou.ddns.net/task_manager/v1";
    private List<TaskInfo> taskInfoArrayList;
    public class TaskInfo {
        int id;
        String task;
        String status;
        String createdAt;
    }

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
        Button uploadButton = (Button)findViewById(R.id.upload);
        uploadButton.setOnClickListener(this);

        Button selectButton = (Button)findViewById(R.id.select);
        selectButton.setText("选择图片");
        selectButton.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                selectImg();
            }
        });

        showMessage = (TextView)findViewById(R.id.showMessage);
        showMessage.setText("Not Applicable");

        showImage=(ImageView)findViewById(R.id.image);

        File dir = new File(DATABASE_PATH);
        Log.i("IMPORTANT: ",Environment.getExternalStorageState());
        if (!dir.exists()) {
            dir.mkdir();
        }

        File dir3 = new File("/sdcard/Unthreaded/temp");
        if (!dir3.exists()) {
            dir3.mkdir();
        }

        File file = new File(FILE_PATH);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void selectImg() {
        final CharSequence[] items = { "Camera", "Gallery" };
        new AlertDialog.Builder(this).setTitle("Select Source")
                .setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 1) {
                            Intent intent = new Intent();
                            intent.setType("image/*");
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                                intent.setAction(Intent.ACTION_PICK);
                                intent.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            } else {
                                intent.setAction(Intent.ACTION_GET_CONTENT);
                            }
                            startActivityForResult(intent, 1);
                        } else {
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            startActivityForResult(intent, 1);
                        }
                    }
                }).create().show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Uri uri = data.getData();

            Cursor cursor = getContentResolver().query(uri, null,
                    null, null, null);
            cursor.moveToFirst();
            originalPath = cursor.getString(1);
            cursor.close();

            showMessage.setText(originalPath);
            Log.i("uri",uri.toString());
            Log.i("uri",originalPath);
        }
        super.onActivityResult(requestCode, resultCode, data);
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
        else if(source.getId() == R.id.upload){
            if (isOnline()) {
                new AsyncHttpServer().execute(serverurl);
            }else{
                Toast.makeText(this, getString(R.string.notOnline), Toast.LENGTH_LONG).show();
            }
        }
    }

    String tempMessage = "";

    public class AsyncHttpServer extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... params) {

            tempMessage = "";

            NameValuePair pair1 = new BasicNameValuePair("name", "Peng Rui");
            NameValuePair pair2 = new BasicNameValuePair("email", "123456789@qq.com");
            NameValuePair pair3 = new BasicNameValuePair("password", "ddd");
            List<NameValuePair> pairList = new ArrayList<NameValuePair>();
            pairList.add(pair1);
            pairList.add(pair2);
            pairList.add(pair3);
            try {
                HttpEntity requestHttpEntity = new UrlEncodedFormEntity(pairList);
                // URL使用基本URL即可，其中不需要加参数
                HttpPost httpPost = new HttpPost(serverurl + "/register");
                // 将请求体内容加入请求中
                httpPost.setEntity(requestHttpEntity);
                // 需要客户端对象来发送请求
                HttpClient httpClient = new DefaultHttpClient();
                // 发送请求
                HttpResponse response = httpClient.execute(httpPost);
                StatusLine a = response.getStatusLine();
                Log.i("IMPORTANT", "POST request: Status code = " + a.getStatusCode());
                showResult(response);
            } catch (Exception e) {
                e.printStackTrace();
            }


            // 使用GET方法发送请求,需要把参数加在URL后面，用？连接，参数之间用&分隔
            String url1 = serverurl + "/tasks";
            // 生成请求对象
            HttpGet httpGet = new HttpGet(url1);
            HttpClient httpClient = new DefaultHttpClient();
            //添加header信息
            httpGet.addHeader("Authorization","91c9bfa10ff21db168154fe3ab064b95");
            // 发送请求
            try{
                HttpResponse response = httpClient.execute(httpGet);
                StatusLine a = response.getStatusLine();
                Log.i("IMPORTANT", "GET request: Status code = " + a.getStatusCode());
                showResult(response);
            }catch (Exception e){
                e.printStackTrace();
            }


            //创建一个http客户端
            HttpClient client = new DefaultHttpClient();
            //创建一个PUT请求
            HttpPut httpPut = new HttpPut(serverurl + "/tasks/2");
            httpPut.addHeader("Authorization","91c9bfa10ff21db168154fe3ab064b95");
            //组装数据放到HttpEntity中发送到服务器
            List<NameValuePair> dataList = new ArrayList<NameValuePair>();
            dataList.add(new BasicNameValuePair("task", "Test Task 100"));
            dataList.add(new BasicNameValuePair("status", "0"));
            try {
                HttpEntity entity = new UrlEncodedFormEntity(dataList);
                httpPut.setEntity(entity);
                //向服务器发送PUT请求并获取服务器返回的结果，可能是修改成功，或者失败等信息
                HttpResponse response = client.execute(httpPut);
                StatusLine a = response.getStatusLine();
                Log.i("IMPORTANT", "PUT request: Status code = " + a.getStatusCode());
                showResult(response);
            }catch(Exception e){
                e.printStackTrace();
            }


            HttpClient client2 = new DefaultHttpClient();
            HttpDelete httpDelete = new HttpDelete(serverurl + "/tasks/4");
            httpDelete.addHeader("Authorization","91c9bfa10ff21db168154fe3ab064b95");
            try {
                HttpResponse response = client2.execute(httpDelete);
                StatusLine statusLine = response.getStatusLine();
                Log.i("IMPORTANT", "DELETE request: Status code = " + statusLine.getStatusCode());
                showResult(response);
            }catch(Exception e){
                e.printStackTrace();
            }

            try {
                uploadByCommonPost();
                downloadImage();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 0;
        }

        protected void onPostExecute(Integer result){
            showImage.setImageBitmap(bitmap);
            //showMessage.setText(tempMessage);
        }
    }

    private final String TAG = "UploadActivity";
    private String path = "/sdcard/Unthreaded/temp/11.bmp";
    private String uploadUrl = serverurl + "/upload";
    private  String originalPath = "/sdcard/DCIM/Camera/20150403_173722.jpg";

    private void uploadByCommonPost() throws IOException {

        savePic(getimage(originalPath));

        String end = "\r\n";
        String twoHyphens = "--";
        String boundary = "******";
        URL url = new URL(uploadUrl);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url
                .openConnection();
        httpURLConnection.setChunkedStreamingMode(128 * 1024);// 128K
        // 允许输入输出流
        httpURLConnection.setDoInput(true);
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setUseCaches(false);
        // 使用POST方法
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
        httpURLConnection.setRequestProperty("Charset", "UTF-8");
        httpURLConnection.setRequestProperty("Content-Type",
                "multipart/form-data;boundary=" + boundary);

        DataOutputStream dos = new DataOutputStream(
                httpURLConnection.getOutputStream());
        dos.writeBytes(twoHyphens + boundary + end);
        dos.writeBytes("Content-Disposition: form-data; name=\"uploadfile\"; filename=\""
                + path.substring(path.lastIndexOf("/") + 1) + "\"" + end);
        dos.writeBytes(end);

        FileInputStream fis = new FileInputStream(path);
        byte[] buffer = new byte[8192]; // 8k
        int count = 0;
        // 读取文件
        while ((count = fis.read(buffer)) != -1) {
            dos.write(buffer, 0, count);
        }
        fis.close();
        dos.writeBytes(end);
        dos.writeBytes(twoHyphens + boundary + twoHyphens + end);
        dos.flush();
        InputStream is = httpURLConnection.getInputStream();
        InputStreamReader isr = new InputStreamReader(is, "utf-8");
        BufferedReader br = new BufferedReader(isr);
        String result = br.readLine();
        Log.i(TAG, result);
        dos.close();
        is.close();
    }

    public  void savePic(Bitmap b) {

        FileOutputStream fos = null;
        try {
            Log.i(TAG,"start savePic");

//         String sdpath ="/storage/sdcard1/";
            File f = new File("/sdcard/Unthreaded/temp" ,"/11.bmp");
            if (f.exists()) {
                f.delete();
            }

            fos = new FileOutputStream(f);
            Log.i(TAG,"strFileName 1= " + f.getPath());
            if (null != fos) {
                b.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
                Log.i(TAG,"save pic OK!");
            }
        } catch (FileNotFoundException e) {
            Log.i(TAG,"FileNotFoundException");
            e.printStackTrace();
        } catch (IOException e) {
            Log.i(TAG,"IOException");
            e.printStackTrace();
        }
    }

    private Bitmap bitmap=null;

    private void downloadImage(){
        try{
            URL url = new URL(serverurl + "/uploads/hhh.bmp");
            HttpURLConnection conn  = (HttpURLConnection)url.openConnection();
            conn.setDoInput(true);
            conn.connect();
            InputStream inputStream=conn.getInputStream();
            Log.i("Important::: ", "debug1");
            bitmap = BitmapFactory.decodeStream(inputStream);
            //bitmap = comp(bitmap);

            Log.i("Important::: ", "debug2");
            //Message msg=new Message();
            //msg.what=1;
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Bitmap getimage(String srcPath) {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        //开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(srcPath,newOpts);//此时返回bm为空

        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        //现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
        float hh = 800f;//这里设置高度为800f
        float ww = 450f;//这里设置宽度为480f
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;//be=1表示不缩放
        if (w > h && w > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0)
            be = 1;
        newOpts.inSampleSize = be;//设置缩放比例
        //重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
        return bitmap;
    }

    private void showResult(HttpResponse response){
        try {
            HttpEntity receivedEntity = response.getEntity();
            InputStream receivedStream = receivedEntity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    receivedStream));
            String result = "";
            String line = "";
            while (null != (line = reader.readLine())) {
                result += line;
            }
            Log.i("IMPORTANT", "Content = " + result);
            tempMessage = tempMessage + "\n" + result;
            jsonAnalysis(result);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void jsonAnalysis(String input) throws JSONException {
        JSONObject obj = new JSONObject(input);
        String hehe1 = obj.getString("error");
        Log.i("IMPORTANT::::: ",hehe1);
        if(!obj.isNull("message")) {
            String hehe2 = obj.getString("message");
            Log.i("IMPORTANT::::: ", hehe2);
        }
        if(!obj.isNull("tasks")){
            JSONArray taskArray = obj.getJSONArray("tasks");
            taskInfoArrayList = new ArrayList<TaskInfo>();
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            taskInfoArrayList = Arrays.asList(gson.fromJson(taskArray.toString(), TaskInfo[].class));
            Log.i("IMPORTANT:::::length: ", ""+taskInfoArrayList.size());
            Log.i("IMPORTANT:::::name: ", taskInfoArrayList.get(0).task);
        }
    }

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

}