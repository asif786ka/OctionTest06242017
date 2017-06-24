package org.octiontest.auction.tabs;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.octiontest.auction.tabs.models.OctionModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

/* Fragment used as page 1 */
public class CurrentFragment extends Fragment {

    private final String URL_TO_HIT = "https://dev-us-02.oction.co/api/v1/auctions";
    private TextView tvData;
    private ListView lvMovies;
    private ProgressDialog dialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_page1, container, false);
        dialog = new ProgressDialog(getActivity());
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setMessage("Loading. Please wait..."); // showing a dialog for loading the data
        // Create default options which will be used for every
        //  displayImage(...) call if no options will be passed to this method
        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getActivity())
                .defaultDisplayImageOptions(defaultOptions)
                .build();
        ImageLoader.getInstance().init(config); // Do it on Application start

        lvMovies = (ListView) rootView.findViewById(R.id.lvMovies);


        // To start fetching the data when app start, uncomment below line to start the async task.
        new JSONTask().execute(URL_TO_HIT);
        return rootView;

    }

    @Override
    public void onResume() {
        super.onResume();
        new JSONTask().execute(URL_TO_HIT);
    }

    public class JSONTask extends AsyncTask<String,String, List<OctionModel> >{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.show();
        }

        @Override
        protected List<OctionModel> doInBackground(String... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();
                String line ="";
                while ((line = reader.readLine()) != null){
                    buffer.append(line);
                }

                String finalJson = buffer.toString();



                List<OctionModel> movieModelList = new ArrayList<>();

                OctionModel movieModel;
                JSONArray parentObject = new JSONArray(finalJson);
                for(int i=0; i<parentObject.length(); i++) {

                    JSONObject aObject = parentObject.getJSONObject(i).getJSONObject("auction");
                    String title = aObject.getString("title");
                    String price = aObject.getString("productPrice");


                    JSONArray pObject = parentObject.getJSONObject(i).getJSONArray("media");

                    String image = pObject.getJSONObject(0).getString("media");
                    //String parentArray = parentObject.getJSONObject(i).getJSONArray("media").getJSONObject(1).toString();

                    movieModel = new OctionModel();
                    movieModel.setImage(image);
                    movieModel.setTitle(title);
                    movieModel.setProductPrice(price);
                    movieModelList.add(movieModel);

                }

                return movieModelList;

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if(connection != null) {
                    connection.disconnect();
                }
                try {
                    if(reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return  null;
        }

        @Override
        protected void onPostExecute(final List<OctionModel> result) {
            super.onPostExecute(result);
            dialog.dismiss();
            if(result != null) {
                MovieAdapter adapter = new MovieAdapter(getActivity(), R.layout.row, result);
                lvMovies.setAdapter(adapter);
            } else {
                Toast.makeText(getActivity(), "Not able to fetch data from server, please check url.", Toast.LENGTH_SHORT).show();
            }
        }
    }



    public class MovieAdapter extends ArrayAdapter{

        private List<OctionModel> movieModelList;
        private int resource;
        private LayoutInflater inflater;
        public MovieAdapter(Context context, int resource, List<OctionModel> objects) {
            super(context, resource, objects);
            movieModelList = objects;
            this.resource = resource;
            inflater = (LayoutInflater) getActivity().getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder = null;

            if(convertView == null){
                holder = new ViewHolder();
                convertView = inflater.inflate(resource, null);
                holder.ivMovieIcon = (ImageView)convertView.findViewById(R.id.ivIcon);
                holder.tvMovie = (TextView)convertView.findViewById(R.id.tvMovie);
                holder.tvTagline = (TextView)convertView.findViewById(R.id.tvTagline);
                holder.tvYear = (TextView)convertView.findViewById(R.id.tvYear);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final ProgressBar progressBar = (ProgressBar)convertView.findViewById(R.id.progressBar);

            // Then later, when you want to display image
            final ViewHolder finalHolder = holder;
            ImageLoader.getInstance().displayImage(movieModelList.get(position).getImage(), holder.ivMovieIcon, new ImageLoadingListener() {
                @Override
                public void onLoadingStarted(String imageUri, View view) {
                    progressBar.setVisibility(View.VISIBLE);
                    finalHolder.ivMovieIcon.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                    progressBar.setVisibility(View.GONE);
                    finalHolder.ivMovieIcon.setVisibility(View.VISIBLE);
                }

                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    progressBar.setVisibility(View.GONE);
                    finalHolder.ivMovieIcon.setVisibility(View.VISIBLE);
                }

                @Override
                public void onLoadingCancelled(String imageUri, View view) {
                    progressBar.setVisibility(View.GONE);
                    finalHolder.ivMovieIcon.setVisibility(View.VISIBLE);
                }
            });

            holder.tvMovie.setText(movieModelList.get(position).getTitle());
            holder.tvTagline.setText(movieModelList.get(position).getProductPrice());
            return convertView;
        }


        class ViewHolder{
            private ImageView ivMovieIcon;
            private TextView tvMovie;
            private TextView tvTagline;
            private TextView tvYear;
            private TextView tvDuration;
            private TextView tvDirector;
            private RatingBar rbMovieRating;
            private TextView tvCast;
            private TextView tvStory;
        }

    }

}

