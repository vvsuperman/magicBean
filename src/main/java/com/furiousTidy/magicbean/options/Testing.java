package com.furiousTidy.magicbean.options;

public class Testing {

    public static void main(String[] args){
        OptionDetails optionDetails = new OptionDetails(true,1800,1639.13,0.05,0.4, 74.2);
        System.out.println(BlackScholesGreeks.calculate(optionDetails));


    }
}
