/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jb.parser.api;

/**
 *
 * @author vkvashin
 */
public class DefaultParseErrorListener implements ParseErrorListener {
    @Override
    public void error(Exception e) {
        e.printStackTrace();
    }

    @Override
    public void error(String text) {
        System.err.println(text);
    }
}
