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
public interface ParseErrorListener {
    void error(Exception e);
    void error(String text);
}
