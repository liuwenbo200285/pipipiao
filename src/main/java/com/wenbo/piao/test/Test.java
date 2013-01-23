package com.wenbo.piao.test;

import java.io.File;
import java.io.IOException;

public class Test {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
        ImgIdent ident = new ImgIdent(new File("/Users/wenbo/Downloads/passCodeAction.jpeg"));
        String validate = ident.getValidatecode(4);
        System.out.println(validate);
	}

}
