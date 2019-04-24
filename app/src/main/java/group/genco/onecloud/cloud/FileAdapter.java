package group.genco.onecloud.cloud;

import android.app.Activity;
import android.content.Context;
import android.view.*;
import android.widget.*;

import java.util.List;

import group.genco.onecloud.R;

public class FileAdapter extends ArrayAdapter<File> {

    Context context;
    int layoutResourceId;
    List<File> data = null;

    public FileAdapter(Context context, int layoutResourceId, List<File> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        FileHolder holder = null;

        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new FileHolder();
            holder.imgIcon = row.findViewById(R.id.listViewItemImage);
            holder.txtTitle = row.findViewById(R.id.listViewItemText);

            row.setTag(holder);
        }
        else
        {
            holder = (FileHolder)row.getTag();
        }

        File file = data.get(position);
        holder.txtTitle.setText(file.getName());
        holder.imgIcon.setImageResource(file.getType() == 0 ? R.drawable.ic_folder : R.drawable.ic_file);

        return row;
    }

    static class FileHolder
    {
        ImageView imgIcon;
        TextView txtTitle;
    }
}