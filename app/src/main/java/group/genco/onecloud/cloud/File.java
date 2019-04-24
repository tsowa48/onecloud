package group.genco.onecloud.cloud;

public class File {
    private String _name;
    private int _type;
    private long _size;
    private String _path;
    private String _id;

    public File(int type, String name, long size, String path, String id) {
        _type = type;
        _name = unescapeUnicode(name);//, "utf-8");
        _size = size;
        _path = path;
        _id = id;
    }

    public String getName() {
        return _name;
    }

    public int getType() {
        return _type;
    }

    @Override
    public String toString() {
        return _name;
    }

    private static String unescapeUnicode(String s) {
        StringBuilder sb = new StringBuilder();
        if(s.length() < 6)
            return s;
        for (int i = 0; i + 2 < s.length(); i++) {
            if (s.substring(i, i + 2).equals("\\u")) {
                int codePoint = Integer.parseInt(s.substring(i + 2, i + 6), 16);
                sb.append(Character.toChars(codePoint));
                i += 5;
            } else {
                sb.append(s.substring(i, i + 1));
                if(i + 3 == s.length())
                    sb.append(s.substring(i + 1, i + 3));
            }
        }
        return sb.toString();
    }
}
