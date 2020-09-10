package com.github.wzc789376152.file;

import jcifs.smb.SmbFile;

public class Test {
    public static void main(String[] args){
        try {
            SmbFile file = new SmbFile("smb://weizc:789376152@10.10.10.181/file/");
            if (!file.exists()) {
                System.out.println("no such folder");
            } else {
                SmbFile[] files = file.listFiles();
                for (SmbFile f : files) {
                    System.out.println(f.getName());
                }
            }
        }catch (Exception e){

        }
    }
}
