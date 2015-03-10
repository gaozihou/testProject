package hk.ust.cse.comp4521.unthreaded;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.xml.transform.Result;


public class Main extends Activity implements View.OnClickListener {
    private ProgressBar progressBar;
    private TextView statusText;
    private int completed;
    private int silly = 1;
    //Zhou Xu Tong is Silly

    private class WorkerTask extends AsyncTask<Object, String, Boolean> {
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
   // private Handler handler;
/*
    private Runnable worker = new Runnable() {
        public void run() {
            int l;
            // Initialize the progress bar and the status TextView
            completed = 0;
            // we want to modify the progress bar so we need to do it from the UI thread
            // how can we make sure the code runs in the UI thread? use the handler!
            handler.post(new Runnable() {
                public void run() {
                    // Here the worker thread uses the post method to initialize the progress bar.
                    // Finish this method by yourself.
                    progressBar.setProgress(completed);
                    statusText.setText(String.format("Completed %d", completed));
                }
            });
            // Here the worker thread do the loop and update the progress bar periodically
            // Similarly, it uses the handler.post method to update the progress bar
            // Finish this part by yourself.
            for (int i = 0; i< 100; ++i) {
                for (int j = 0; j < 5000; ++j) {
                    for (int k = 0; k < 5000; ++k) {
                        l = i * j * k;
                    }
                }
                completed += 1;
                handler.post(new Runnable() {
                    public void run() {
                        // Here the worker thread uses the post method to initialize the progress bar.
                        // Finish this method by yourself.
                        progressBar.setProgress(completed);
                        statusText.setText(String.format("Completed %d", completed));
                    }
                });
            }
        }
    };
    */

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
}