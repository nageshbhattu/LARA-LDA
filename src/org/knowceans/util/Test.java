/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.knowceans.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author NIT-ANDHRA
 */
public class Test {
    public void LoadDirectory(String path, String suffix)  {
        File dir = new File(path);
        
        for (File f : dir.listFiles()) {
            if (f.isFile() && f.getName().endsWith(suffix)) {
                LoadReviews(f.getAbsolutePath());
            } else if (f.isDirectory()) {
                LoadDirectory(f.getAbsolutePath(), suffix);
            }
        }
        System.out.println("Loading " + " hotels from " + path);
        for(String key:hm.keySet()){
            System.out.println(key + ":"+hm.get(key));
        }
    }
    HashMap<String,Integer> hm = new HashMap<>();
    int count =0;
    public void LoadReviews(String fileName) {
        BufferedReader reader = null;
        try {
            File f = new File(fileName);
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
            String tmpTxt;
            int review_size = 0;
            while ((tmpTxt = reader.readLine()) != null) {
                Pattern pat = Pattern.compile("^<([a-zA-Z ]*?)>");
                Matcher mat = pat.matcher(tmpTxt);
                if(mat.find()){
                    if(hm.containsKey(mat.group(1))){
                        
                    }else{
                        hm.put(mat.group(1), count++);
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    public static void main(String[] args){
        Test test = new Test();
        test.LoadDirectory("C:\\Users\\NIT-ANDHRA\\Desktop\\Projects\\2019\\LDA\\Review_Texts", "dat");
    }
}
