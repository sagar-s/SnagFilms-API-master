package com.snag.films;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    ListView list;
    FilmsAdapter adapter;
    public static ArrayList<HashMap<String, String>> filmsArray = new ArrayList<>();
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(com.snag.films.R.layout.activity_main);
        list = (ListView) findViewById(com.snag.films.R.id.list);
        adapter = new FilmsAdapter(context);
        list.setAdapter(adapter);
        final AsyncTask<String, Integer, String> execute = new ReadFromFeed().execute();
    }

    class ReadFromFeed extends AsyncTask<String, Integer, String> {
        ProgressDialog progressDialog;

        public ProgressDialog createProgressDialog(Context context) {
            ProgressDialog dialog = new ProgressDialog(context);
            try {
                dialog.show();
            } catch (WindowManager.BadTokenException e) {
                e.printStackTrace();
            }
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.setCancelable(false);
            dialog.setContentView(com.snag.films.R.layout.progressdialog);
            return dialog;
        }

        @Override
        public void onPreExecute() {
            super.onPreExecute();
            progressDialog = createProgressDialog(context);
            progressDialog.show();
        }

        @Override
        public String doInBackground(String... params) {
            HashMap<String, String> values = new HashMap<>();
            URL url;
            String response = "";
            try {
                url = new URL("http://snagfilms.com/apis/films.json?limit=10");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(60000);
                conn.setConnectTimeout(60000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(getPostData(values));
                writer.flush();
                writer.close();
                os.close();
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    String line;
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        response += line;
                    }
                } else {
                    response = "";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response;
        }

        String getPostData(HashMap<String, String> params) throws UnsupportedEncodingException {
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (first)
                    first = false;
                else
                    result.append("&");

                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
            return result.toString();
        }

        @Override
        public void onPostExecute(String result) {
            super.onPostExecute(result);
            if (progressDialog.isShowing())
                progressDialog.dismiss();
            try {
                JSONObject jO = new JSONObject(result);
                JSONObject jO1 = jO.getJSONObject("films");
                JSONArray jsonArray = jO1.getJSONArray("film");
                filmsArray.clear();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject subObj = jsonArray.getJSONObject(i);
                    HashMap<String, String> map = new HashMap<>();
                    map.put("id", subObj.optString("id"));
                    map.put("title", subObj.optString("title"));
                    map.put("time", subObj.optString("durationMinutes") + ":" + subObj.optString("durationSeconds"));
                    map.put("permaLink", subObj.optString("permaLink"));
                    map.put("views", subObj.optString("views"));
                    JSONObject subsubObj1 = subObj.getJSONObject("images");
                    JSONArray subsubJArr1 = subsubObj1.getJSONArray("image");
                    JSONObject deepObj1 = subsubJArr1.getJSONObject(0);
                    map.put("image", deepObj1.getString("src"));
                    filmsArray.add(map);
                }
                adapter.notifyDataSetChanged();
            } catch (JSONException jse) {
                jse.printStackTrace();
                Toast.makeText(context, "Internet not available.", Toast.LENGTH_LONG).show();
            }
        }
    }

    class FilmsAdapter extends BaseAdapter {
        TextView tv1, tv2, tv3;
        ImageView image;
        LayoutInflater li;
        Context c;

        FilmsAdapter(Context ct) {
            li = LayoutInflater.from(ct);
            c = ct;
        }

        @Override
        public int getCount() {
            return filmsArray.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            convertView = li.inflate(com.snag.films.R.layout.film_layout, null);
            tv1 = (TextView) convertView.findViewById(com.snag.films.R.id.tv1);
            tv2 = (TextView) convertView.findViewById(com.snag.films.R.id.tv2);
            tv3 = (TextView) convertView.findViewById(com.snag.films.R.id.tv3);
            image = (ImageView) convertView.findViewById(com.snag.films.R.id.image);
            tv1.setText(filmsArray.get(position).get("title"));
            Picasso.with(context).load(filmsArray.get(position).get("image")).into(image);
            tv2.setText("Time: " + filmsArray.get(position).get("time"));
            tv3.setText("Views: " + filmsArray.get(position).get("views"));

            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(context, Webpage.class);
                    i.putExtra("url", filmsArray.get(position).get("permaLink"));
                    startActivity(i);
                }
            });
            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }
}
















