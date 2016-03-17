// Copyright 2015 theaigames.com (developers@theaigames.com)

//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at

//        http://www.apache.org/licenses/LICENSE-2.0

//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//  
//    For the full copyright and license information, please view the LICENSE
//    file that was distributed with this source code.

import java.util.Scanner;
import java.io.*;
/**
 * MyBot class
 * 
 * Main class that will keep reading output from the engine.
 * Will either update the bot state or get actions.
 * 
 * @author Jim van Eeden <jim@starapple.nl>, Joost de Meij <joost@starapple.nl>
 */

public class BotParser {
    
	final Scanner scan;
    final Adaptor ad;
    public static int mBotId = 0;
    public int cols = 1, rows = 1;
    
    public BotParser() {
        try{
       // File file = new File("input.txt");
        
            this.scan = new Scanner(System.in);
		// this.scan = new Scanner(file);
	    
        this.ad = new Adaptor();
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
    
    public void run() {
        while(scan.hasNextLine()) {
            String line = scan.nextLine();
         //   System.out.println(line);
            if(line.length() == 0) {
                continue;
            }

            String[] parts = line.split(" ");
            
            if(parts[0].equals("settings")) {
                if (parts[1].equals("field_columns")) {
                    cols = Integer.parseInt(parts[2]);
                    ad.setColumns(cols);
                }
                if (parts[1].equals("field_rows")) {
                    rows = Integer.parseInt(parts[2]);
                    ad.setRows(rows);
                }
                if (parts[1].equals("your_botid")) {
                    mBotId = Integer.parseInt(parts[2]);
                    ad.setBotId(mBotId);
                }
            } else if(parts[0].equals("update")) { /* new field data */
                if (parts[2].equals("field")) {
                    String data = parts[3];
                    ad.update(parseFromString(data)); /* Parse Field with data */
                }
            } else if(parts[0].equals("action")) {
                if (parts[1].equals("move")) { /* move requested */
                    int time = Integer.parseInt(parts[2]);
                    int column = ad.makeTurn(time);
                    System.out.println("place_disc " + column);
                }
            }
            else { 
                System.out.println("unknown command");
            }
        }
    }

    /**
     * Initialise field from comma separated String
     * @param String : 
    */
    public int[][] parseFromString(String s) {
        int[][] mBoard =  new int[cols][rows];
        s = s.replace(';', ',');
        String[] r = s.split(",");
        int counter = 0;
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                mBoard[x][y] = Integer.parseInt(r[counter]); 
                counter++;
            }
        }
        return mBoard;
    }
}