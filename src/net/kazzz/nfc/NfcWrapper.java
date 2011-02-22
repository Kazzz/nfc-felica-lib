/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.kazzz.nfc;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.kazzz.felica.FeliCaException;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Parcelable;

/**
 * Nfcの隠し(@hide)機能を使うためのリフレクション機能をラップするラッパークラスを提供します
 * 
 * @author Kazzz
 * @date 2011/02/19
 * @since Android API Level 9
 *
 */

public class NfcWrapper {
    static final String TAG = "NfcWrapper";
    private NfcWrapper() {}
    
    /**
     * タグがNDEFフォーマッか否かを検査します
     * @param tag インテントから取得したタグ(ndroid.nfc.Tag)をセット
     * @return boolean タグがNDefフォーマットの場合はtrueが戻ります
     * @throws NfcException
     */
    public static final boolean isNdef(Parcelable tag) throws NfcException {
        return (Boolean) invokeNfcTagMethod("isNdef", tag);
    }
    /**
     * NDEFメッセージを取得します
     * @param tag インテントから取得したタグ(ndroid.nfc.Tag)をセット
     * @return NdefMessage 取得したNdefメッセージが戻ります
     * @throws NfcException
     */
    public static final NdefMessage read(Parcelable tag) throws NfcException {
        Object result = invokeNfcTagMethod("read", tag);
        return result != null ? (NdefMessage) result : null;
    }
    /**
     * NDEFメッセージを書き込みます
     * @param tag インテントから取得したタグ(ndroid.nfc.Tag)をセット
     * @param msg 書き込みするNdefMessageをセット
     * @return int 書き込んだ長さが戻ります (書きこみに失敗した場合は-1が戻ります
     * @throws NfcException
     */
    public static final int write(Parcelable tag, NdefMessage msg) throws NfcException {
        return (Integer) invokeNfcTagMethod("write", tag, msg);
    }
   /**
     * INfcTag#transceiveを実行します
     * 
     * @param Tag 隠しクラスである android.nfc.Tag クラスの参照をセットします
     * @param commandPacket 実行するコマンドパケットをセットします
     * @return byte[] コマンドの実行結果バイト列で戻ります 
     * @throws FeliCaException コマンドの発行に失敗した場合にスローされます
     */
    public static final byte[] transceive(Parcelable tag, byte[] data) throws NfcException {
        return (byte[]) invokeNfcTagMethod("transceive", tag, data);
    }
    /**
     * INfcTagインタフェースのメソッドをリフレクションを使って起動します
     * 
     * <pre>Android 2.3の隠しクラス(@hide)に依存しています。今後の仕様変更で使えなくなるリスクを考慮してください</pre>
     * 
     * @param methodName 実行するメソッド名をセット
     * @param tag デバイスから取得したタグ( "android.nfc.extra.TAG" )をセットします
     * @param params メソッドの引数をセット
     * @return メソッドの実行結果が戻ります
     * @throws NfcException
     */
    public static final Object invokeNfcTagMethod(String methodName, Parcelable tag, Object... params) throws NfcException {
        try {
            NfcAdapter adapter = NfcAdapter.getDefaultAdapter();
            Class<?> tagClass = Class.forName("android.nfc.Tag");
            
            // android.nfc.RawTagConnectionを生成
            Method createRawTagConnection = 
                adapter.getClass().getMethod("createRawTagConnection", tagClass);

            // android.nfc.RawTagConnection#mTagServiceフィールドを取得 (NfcService.INfcTagへの参照が入っている)
            Object rawTagConnection = createRawTagConnection.invoke(adapter, tag);
            Field f = rawTagConnection.getClass().getDeclaredField("mTagService");
            f.setAccessible(true);
            Object tagService = f.get(rawTagConnection);

            //データの型とデータを配列にまとめる
            Class<?>[] typeArray = new Class[params.length+1];
            typeArray[0] = Integer.TYPE;
            Object[] paramArray = new Object[params.length+1];
            paramArray[0] = getServiceHandle(adapter, tag);
            
            for ( int i = 0; i < params.length ; i++ ) {
                typeArray[i+1] = params[i].getClass();
                paramArray[i+1] = params[i];
            }
            
            Method method = tagService.getClass().getMethod(methodName, typeArray);
            Object o = method.invoke(tagService, paramArray);
            return o;
        } catch (ClassNotFoundException e){
            throw new NfcException(e);
        } catch (NoSuchMethodException e){
            throw new NfcException(e);
        } catch (SecurityException e){
            throw new NfcException(e);
        } catch (NoSuchFieldException e){
            throw new NfcException(e);
        } catch (IllegalAccessException e){
            throw new NfcException(e);
        } catch (IllegalArgumentException e){
            throw new NfcException(e);
        } catch (InvocationTargetException e){
            throw new NfcException(e);
        }            
    }
    /**
     * NFCデバイスへアクセスするためのサービスハンドルを取得します
     * 
     * @param adapter NfcAdapterへの参照をセット
     * @param tag  デバイスから取得したタグ( "android.nfc.extra.TAG" )をセットします
     * @return int 取得したサービスハンドルが戻ります
     * @throws ClassNotFoundException
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public static final int getServiceHandle(NfcAdapter adapter, Parcelable tag) throws 
        ClassNotFoundException , SecurityException
        , NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        //ServiceHandleをmServiceHandleフィールドから取得
        Class<?> tagClass = Class.forName("android.nfc.Tag");
        Field f = tagClass.getDeclaredField("mServiceHandle");
        f.setAccessible(true);
        return (Integer) f.get(tag);  
    }
}
