package com.example.demo.methodinterceptproxy;

import android.content.Context;
import android.widget.Toast;

/**
 * <pre>
 *     author : zhangke
 *     e-mail : zhangke3016@gmail.com
 *     time   : 2017/05/08
 *     desc   :
 * </pre>
 */

public class Test {

    public void toast1(Context ctx){
        Toast.makeText(ctx, "--111111----", Toast.LENGTH_SHORT).show();
    }
}
