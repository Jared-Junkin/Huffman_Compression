import java.util.HashMap;
import java.util.Map;

public class HuffmanDecoding {
    private HashMap<String, String> makeDict(String[] dictionary){
        String letDict = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        HashMap<String, String> ret = new HashMap<>();
        for(int k = 0; k < dictionary.length; k++){
            ret.put(dictionary[k], letDict.substring(k, k+1)); //the binary, then the letter.
        }
        return ret;
    }
    public String decode(String archive, String[] dictionary) {
        HashMap<String, String> myMap = makeDict(dictionary);
        StringBuilder ret = new StringBuilder();
        for(int k = 0; k < archive.length() - 1; k++){
            for(int j = k; j < archive.length() - 1; j++){
                String decoding = archive.substring(k, j);
                if(myMap.containsKey(decoding)){ //if the binary sequence decoding is in myMap...
                    ret.append(myMap.get(decoding));
                }
            }
        }
        return ret.toString();
    }
}