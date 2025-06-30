/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package scr;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Mario
 */
public class Sensoristica {
    
    List<List<Object>> sensori = new ArrayList<>();
    
    public Sensoristica(){
        for(int i=0; i<15; i++){
            sensori.add(new ArrayList<>());
        }
    }
    
    public void leggiCSV(String percorsoFile){
        try(BufferedReader br = new BufferedReader(new FileReader(percorsoFile))){
            String riga;
            riga = br.readLine();
            
            while(true){
                
                riga = br.readLine();
                if(riga==null){
                    break;
                }
                
                String[] valori = riga.split(";");
                assert(valori.length == 15);
                
                for(int i=0; i<15; i++){
                    try{
                        if(i==5 || i==12 || i==13){
                            Integer tmp = Integer.parseInt(valori[i]);
                            sensori.get(i).add(tmp);
                        }
                        else{
                            Double tmp = Double.parseDouble(valori[i]);
                            sensori.get(i).add(tmp);
                        }
                    }
                    catch(NumberFormatException e){
                        System.err.println(e.getMessage());
                    }
                }
                
            }
        }
        catch(IOException e){
            System.err.println(e.getMessage());
        }
    }
    /*speed = 1
    angle to track 2
    track pos 3
    lateral speed 6
    rpm 86
    gear 4
    i trackedge a 45° e centrale.*/
    
}
