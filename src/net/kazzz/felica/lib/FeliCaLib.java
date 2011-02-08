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
package net.kazzz.felica.lib;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.kazzz.felica.FeliCaException;
import net.kazzz.felica.IFeliCaByteData;
import net.kazzz.felica.command.IFeliCaCommand;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.util.Log;

/**
 * FeliCaカードにアクセスするためのデータと操作をライブラリィとして提供します
 * 
 * <pre>
 * ※ 「FeliCa」は、ソニー株式会社が開発した非接触ICカードの技術方式です。
 * ※ 「FeliCa」、「FeliCaポケット」、「FeliCaランチャー」は、ソニー株式会社の登録商標です。
 * ※ 「Suica」は東日本旅客鉄道株式会社の登録商標です。
 * ※ 「PASMO」は、株式会社パスモの登録商標です。
 * 
 * 本ライブラリィはFeliCa、ソニー株式会社とはなんの関係もありません。
 * </pre>
 * 
 * @author Kazzz
 * @date 2011/01/16
 * @since Android API Level 9
 *
 */

public class FeliCaLib {
    static final String TAG = "FeliCaLib";
    
    //polling
    public static final byte COMMAND_POLLING = 0x00;
    public static final byte RESPONSE_POLLING = 0x01;

    //request service
    public static final byte COMMAND_REQUEST_SERVICE = 0x02;
    public static final byte RESPONSE_REQUEST_SERVICE = 0x03;

    //request RESPONSE
    public static final byte COMMAND_REQUEST_RESPONSE = 0x04;
    public static final byte RESPONSE_REQUEST_RESPONSE = 0x05;

    //read without encryption
    public static final byte COMMAND_READ_WO_ENCRYPTION = 0x06;
    public static final byte RESPONSE_READ_WO_ENCRYPTION = 0x07;

    //write without encryption
    public static final byte COMMAND_WRITE_WO_ENCRYPTION = 0x08;
    public static final byte RESPONSE_WRITE_WO_ENCRYPTION = 0x09;

    //search service code
    public static final byte COMMAND_SEARCH_SERVICECODE = 0x0a;
    public static final byte RESPONSE_SEARCH_SERVICECODE = 0x0b;

    //request system code
    public static final byte COMMAND_REQUEST_SYSTEMCODE = 0x0c;
    public static final byte RESPONSE_REQUEST_SYSTEMCODE = 0x0d;

    //authentication 1
    public static final byte COMMAND_AUTHENTICATION1 = 0x10;
    public static final byte RESPONSE_AUTHENTICATION1 = 0x11;

    //authentication 2
    public static final byte COMMAND_AUTHENTICATION2 = 0x12;
    public static final byte RESPONSE_AUTHENTICATION2 = 0x13;

    //read
    public static final byte COMMAND_READ = 0x14;
    public static final byte RESPONSE_READ = 0x15;

    //write
    public static final byte COMMAND_WRITE = 0x16;
    public static final byte RESPONSE_WRITE = 0x17;

    
    public static final Map<Byte, String> commandMap = new HashMap<Byte, String>();
    
    //command code and name dictionary
    static {
        commandMap.put(COMMAND_POLLING, "Polling");
        commandMap.put(RESPONSE_POLLING, "Polling(responce)");
        commandMap.put(COMMAND_REQUEST_SERVICE, "Request Service");
        commandMap.put(RESPONSE_REQUEST_SERVICE, "Request Service(response)");
        commandMap.put(COMMAND_REQUEST_RESPONSE, "Request Response");
        commandMap.put(RESPONSE_REQUEST_RESPONSE, "Request Response(response)");
        commandMap.put(COMMAND_READ_WO_ENCRYPTION, "Read Without Encryption");
        commandMap.put(RESPONSE_READ_WO_ENCRYPTION, "Read Without Encryption(response)");
        commandMap.put(COMMAND_WRITE_WO_ENCRYPTION, "Write Without Encryption");
        commandMap.put(RESPONSE_WRITE_WO_ENCRYPTION, "Write Without Encryption(response)");
        commandMap.put(COMMAND_SEARCH_SERVICECODE, "Search Service");
        commandMap.put(RESPONSE_SEARCH_SERVICECODE, "Search Service(response)");
        commandMap.put(COMMAND_REQUEST_SYSTEMCODE, "Request System Code");
        commandMap.put(RESPONSE_REQUEST_SYSTEMCODE, "Request System Code(response)");
        commandMap.put(COMMAND_AUTHENTICATION1, "Authentication1");
        commandMap.put(RESPONSE_AUTHENTICATION1, "Authentication1(response)");
        commandMap.put(COMMAND_AUTHENTICATION2, "Authentication2");
        commandMap.put(RESPONSE_AUTHENTICATION2, "Authentication2(response)");
        commandMap.put(COMMAND_READ, "Read");
        commandMap.put(RESPONSE_READ, "Read(response)");
        commandMap.put(COMMAND_WRITE, "Write");
        commandMap.put(RESPONSE_WRITE, "Write(response)");
    }
    /**
     * 
     * FeliCa コマンドパケットクラスを提供します
     * 
     * @author Kazzz
     * @date 2011/01/20
     * @since Android API Level 9
     */
    public static class CommandPacket implements IFeliCaCommand {
        protected final byte length;     //全体のデータ長 
        protected final byte commandCode;//コマンドコード
        protected final IDm  idm;        //FeliCa IDm
        protected final byte[] data;     //コマンドデータ
        /**
         * コンストラクタ
         * @param response 他のレスポンスをセット
         */
        public CommandPacket(CommandPacket command) throws FeliCaException {
            this(command.getBytes());
        }
        /**
         * コンストラクタ
         * 
         * @param data コマンドパケット全体を含むバイト列をセット
         * @throws FeliCaException 
         */
        public CommandPacket(final byte[] data) throws FeliCaException {
            this(data[0], Arrays.copyOfRange(data, 1, data.length));
        }
        /**
         * コンストラクタ
         * 
         * @param commandCode コマンドコードをセット
         * @param data コマンドデータをセット (IDmを含みます)
         * @throws FeliCaException 
         */
        public CommandPacket(byte commandCode, final byte... data) throws FeliCaException {
            if ( !commandMap.containsKey(commandCode))
                throw new FeliCaException("commandCode : " + commandCode + " not supported.");
            this.commandCode = commandCode;
            if ( data.length >= 8 ) {
                this.idm = new IDm(Arrays.copyOfRange(data, 0, 8));
                this.data = Arrays.copyOfRange(data, 8, data.length);
            } else {
                this.idm = null;
                this.data = Arrays.copyOfRange(data, 0, data.length);
            }
            this.length = (byte)(data.length + 2);
        }
        /**
         * コンストラクタ
         * 
         * @param commandCode コマンドコードをセット
         * @param idm システム製造ID(IDm)をセット
         * @param data コマンドデータをセット
         * @throws FeliCaException 
         */
        public CommandPacket(byte commandCode, IDm idm, final byte... data) throws FeliCaException {
            if ( !commandMap.containsKey(commandCode))
                throw new FeliCaException("commandCode : " + commandCode + " not supported.");
            this.commandCode = commandCode;
            this.idm = idm;
            this.data = data;
            this.length = (byte)(idm.getBytes().length + data.length + 2);
        }
        /**
         * コンストラクタ
         * 
         * @param commandCode コマンドコードをセット
         * @param idm システム製造ID(IDm)がセットされたバイト配列をセット
         * @param data コマンドデータをセット
         * @throws FeliCaException 
         */
        public CommandPacket(byte commandCode, byte[] idm, final byte... data) throws FeliCaException {
            if ( !commandMap.containsKey(commandCode))
                throw new FeliCaException("commandCode : " + commandCode + " not supported.");
            this.commandCode = commandCode;
            this.idm = new IDm(idm);
            this.data = data;
            this.length = (byte)(idm.length + data.length + 2);
        }
        
        /* (non-Javadoc)
         * @see net.felica.IFeliCaCommand#getIDm()
         */
        @Override
        public IDm getIDm() {
            return this.idm;
        }
        /**
         * バイト列表現を戻します
         * @return byte[] このデータのバイト列表現を戻します
         */
        public byte[] getBytes() {
            ByteBuffer buff = ByteBuffer.allocate(this.length);
            if ( this.idm != null ) {
                buff.put(this.length).put(this.commandCode).put(this.idm.getBytes()).put(this.data);
            } else {
                buff.put(this.length).put(this.commandCode).put(this.data);
            }
            return buff.array();
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
           StringBuilder sb = new StringBuilder();
           sb.append("FeliCa コマンドパケット \n");
           sb.append(" コマンド名:" + commandMap.get(this.commandCode)  +  "\n");
           sb.append(" データ長: " + Util.getHexString(this.length) + "\n");
           sb.append(" コマンドコード : " + Util.getHexString(this.commandCode) +  "\n");
           if ( this.idm != null )
               sb.append(" " + this.idm.toString() + "\n");
           sb.append(" データ: " + Util.getHexString(this.data) + "\n");
           return sb.toString();
        }

    }
    /**
     * FeliCa コマンドレスポンスクラスを提供します
     * 
     * @author Kazz
     * @since Android API Level 9
     */
    public static class CommandResponse implements IFeliCaCommand {
        protected final byte[] rawData;
        protected final byte length;      //全体のデータ長 (FeliCaには無い)
        protected final byte responseCode;//コマンドレスポンスコード)
        protected final IDm idm;          //FeliCa IDm
        protected final byte[] data;      //コマンドデータ
        
        /**
         * コンストラクタ
         * @param response 他のレスポンスをセット
         */
        public CommandResponse(CommandResponse response) {
            this(response.getBytes());
        }
        /**
         * コンストラクタ
         * 
         * @param data コマンド実行結果で戻ったバイト列をセット
         */
        public CommandResponse(byte[] data) {
            this.rawData = data;
            this.length = data[0]; 
            this.responseCode = data[1];
            this.idm = new IDm(Arrays.copyOfRange(data, 2, 10));
            this.data = Arrays.copyOfRange(data, 10, data.length);
        }
        /* (non-Javadoc)
         * @see net.felica.IFeliCaCommand#getIDm()
         */
        @Override
        public IDm getIDm() {
            return this.idm;
        }
        /**
         * バイト列表現を戻します
         * @return byte[] このデータのバイト列表現を戻します
         */
        public byte[] getBytes() {
            return this.rawData;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
           StringBuilder sb = new StringBuilder();
           sb.append(" \n\n");
           sb.append("FeliCa レスポンスパケット \n");
           sb.append(" コマンド名:" + commandMap.get(this.responseCode)  +  "\n");
           sb.append(" データ長: " + Util.getHexString(this.length) + "\n");
           sb.append(" レスポンスコード: " + Util.getHexString(this.responseCode) + "\n");
           sb.append(" "+ this.idm.toString() + "\n");
           sb.append(" データ: " + Util.getHexString(this.data) + "\n");
           return sb.toString();
        }      
    }
    /**
     * 
     * FeliCa IDmクラスを提供します
     * 
     * @author Kazzz
     * @date 2011/01/20
     * @since Android API Level 9
     */
    public static class IDm implements IFeliCaByteData {
        final byte[] manufactureCode;
        final byte[] cardIdentification;
        /**
         * コンストラクタ 
         * @param bytes IDmの格納されているバイト列をセットします
         */
        public IDm(byte[] bytes) {
            this.manufactureCode = new byte[]{bytes[0], bytes[1]};
            this.cardIdentification = 
                new byte[]{bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7]};
        }
        
        /* (non-Javadoc)
         * @see net.felica.IFeliCaByteData#getBytes()
         */
        @Override
        public byte[] getBytes() {
            ByteBuffer buff = ByteBuffer.allocate(
                    this.manufactureCode.length + this.cardIdentification.length);
            buff.put(this.manufactureCode).put(this.cardIdentification);
            return buff.array();
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("IDm (8byte) : " + Util.getHexString(this.getBytes()) + "\n");
            sb.append(" 製造者コード: " + Util.getHexString(this.manufactureCode) + "\n");
            sb.append(" カード識別番号:\n");
            sb.append("   製造器:" + Util.getHexString(this.cardIdentification, 0, 2) + "\n");
            sb.append("   日付:" + Util.getHexString(this.cardIdentification, 2, 2) + "\n");
            sb.append("   シリアル:" + Util.getHexString(this.cardIdentification, 4, 2) + "\n");
            return sb.toString();
        }

    }
    /**
     * 
     * FeliCa PMmクラスを提供します
     * 
     * @author Kazzz
     * @date 2011/01/20
     * @since Android API Level 9
     */
   public static class PMm implements IFeliCaByteData {
        final byte[] icCode;              // ROM種別, IC種別
        final byte[] maximumResponseTime; // 最大応答時間
        /**
         * コンストラクタ
         * @param bytes バイト列をセット
         */
        public PMm(byte[] bytes) {
            this.icCode = new byte[]{bytes[0], bytes[1]};
            this.maximumResponseTime = 
                new byte[]{bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7]};
        }
        /* (non-Javadoc)
         * @see net.felica.IFeliCaByteData#getBytes()
         */
        @Override
        public byte[] getBytes() {
            ByteBuffer buff = ByteBuffer.allocate(
                    this.icCode.length + this.maximumResponseTime.length);
            buff.put(this.icCode).put(this.maximumResponseTime);
            return buff.array();
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("PMm(製造パラメータ)\n");
            sb.append(" ICコード(2byte): " + Util.getHexString(this.icCode) + "\n");
            sb.append("   ROM種別: " + Util.getHexString(this.icCode, 0, 1) + "\n");
            sb.append("   IC 種別: " + Util.getHexString(this.icCode, 1, 1) + "\n");
            sb.append("\n");
            sb.append(" 最大応答時間パラメタ(6byte)\n");
            sb.append("  B3(request service):" + Util.getBinString(this.maximumResponseTime, 0, 1) + "\n");
            sb.append("  B4(request response):" + Util.getBinString(this.maximumResponseTime, 1, 1) + "\n");
            sb.append("  B5(authenticate):" + Util.getBinString(this.maximumResponseTime, 2, 1) + "\n");
            sb.append("  B6(read):" + Util.getBinString(this.maximumResponseTime, 3, 1) + "\n");
            sb.append("  B7(write):" + Util.getBinString(this.maximumResponseTime, 4, 1) + "\n");
            sb.append("  B8():" + Util.getBinString(this.maximumResponseTime, 5, 1) + "\n");
            return sb.toString();
        }
    }
    
   /**
    * FeliCa SystemCodeクラスを提供します
    * 
    * @author Kazzz
    * @date 2011/01/20
    * @since Android API Level 9
    */
    public static class SystemCode implements IFeliCaByteData {
        final byte[] systemCode;
        /**
         * コンストラクタ
         * @param bytes バイト列をセット
         */
        public SystemCode(byte[] bytes) {
            this.systemCode = bytes;
        }
        /* (non-Javadoc)
         * @see net.felica.IFeliCaByteData#getBytes()
         */
        @Override
        public byte[] getBytes() {
            return this.systemCode;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("システムコード : " + Util.getHexString(this.systemCode) + "\n");
            return sb.toString();
        }
    }
    /**
     * FeliCa ServiceCodeクラスを提供します
     * 
     * @author Kazzz
     * @date 2011/01/20
     * @since Android API Level 9
     */
     public static class ServiceCode implements IFeliCaByteData {
         final byte[] serviceCode;
         /**
          * コンストラクタ
          * @param bytes バイト列をセット
          */
         public ServiceCode(byte[] bytes) {
             this.serviceCode = bytes;
         }
         /* (non-Javadoc)
          * @see net.felica.IFeliCaByteData#getBytes()
          */
         @Override
         public byte[] getBytes() {
             return this.serviceCode;
         }

         /* (non-Javadoc)
          * @see java.lang.Object#toString()
          */
         @Override
         public String toString() {
             StringBuilder sb = new StringBuilder();
             sb.append("サービスコード : " + Util.getHexString(this.serviceCode) + "\n");
             return sb.toString();
         }
     }
    
    /**
     * 
     * Felica FileSystemにおけるService(サービス)クラスを提供します
     * 
     * @author Kazzz
     * @date 2011/01/20
     * @since Android API Level 9
     */
    public class Service implements IFeliCaByteData {
        final ServiceCode[] serviceCodes;
        final BlockListElement[] blockListElements;
        /**
         * コンストラクタ
         * 
         * @param serviceCode サービスコードの配列をセット
         * @param blockListElements ブロックリストエレメントの配列をセット
         */
        public Service(ServiceCode[] serviceCodes, BlockListElement ... blockListElements ) {
            this.serviceCodes = serviceCodes;
            this.blockListElements = blockListElements;
        }
        /* (non-Javadoc)
         * @see net.felica.IFeliCaByteData#getBytes()
         */
        @Override
        public byte[] getBytes() {

            int length = 0;
            for (ServiceCode s : this.serviceCodes ) {
                length += s.getBytes().length;
            }
            
            for (BlockListElement b : blockListElements) {
                length += b.getBytes().length;
            }
            
            ByteBuffer buff = ByteBuffer.allocate(length);
            for (ServiceCode s : this.serviceCodes ) {
                buff.put(s.getBytes());
            }
            
            for (BlockListElement b : blockListElements) {
                buff.put(b.getBytes());
            }
            
            return buff.array();
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (ServiceCode s : this.serviceCodes ) {
                sb.append(s.toString());
            }
            
            for (BlockListElement b : blockListElements) {
                sb.append(b.toString());
            }
            return sb.toString();
        }
    }
    
   
    /**
     * FeliCa FileSystemにおけるBlock(ブロック)を提供します
     * 
     * @since Android API Level 9
     */
    public class Block implements IFeliCaByteData {
        byte[] data = new byte[16];
        /* (non-Javadoc)
         * @see net.felica.IFeliCaByteData#getBytes()
         */
        @Override
        public byte[] getBytes() {
            return this.data;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("データ : " + Util.getHexString(this.data) + "\n");
            return sb.toString();
        }   }
    
    /**
     * Felica FileSystemにおけるBlockListElement(2byte又は3byte)クラスを提供します
     * 
     * @author Kazzz
     * @date 2011/01/20
     * @since Android API Level 9
     */
    public class BlockListElement implements IFeliCaByteData {
        public static final byte LENGTH_2_BYTE = (byte) 0x80;
        public static final byte LENGTH_3_BYTE = (byte) 0x00; 
        public static final byte ACCESSMODE_DECREMENT = 0x00; 
        public static final byte ACCESSMODE_CACHEBACK = 0x01; 
        final byte lengthAndaccessMode; // 
        final byte serviceCodeListOrder; // 
        final byte[] blockNumber;
        
        /**
         * コンストラクタ
         * @param accessMode アクセスモードを0又は1でセット
         * @param serviceCodeListOrder サービスコードリスト順をセット
         * @param blockNumber 対象のブロック番号を1バイト又は2バイトでセット
         */
        public BlockListElement (byte accessMode, byte serviceCodeListOrder, byte... blockNumber ) {
            if ( blockNumber.length > 1 ) {
                this.lengthAndaccessMode =  (byte)(accessMode | LENGTH_2_BYTE & 0xFF);
            } else {
                this.lengthAndaccessMode =  (byte)(accessMode | LENGTH_3_BYTE & 0xFF);
            }
            this.serviceCodeListOrder = (byte) (serviceCodeListOrder & 0x0F);
            this.blockNumber = blockNumber;
        }
        /* (non-Javadoc)
         * @see net.felica.IFeliCaByteData#getBytes()
         */
        @Override
        public byte[] getBytes() {
            if ( (this.lengthAndaccessMode & LENGTH_2_BYTE) == 1 ) {
                ByteBuffer buff = ByteBuffer.allocate(2);
                buff.put( (byte)
                        ((this.lengthAndaccessMode | this.serviceCodeListOrder) & 0xFF))
                    .put(this.blockNumber[0]);
                return buff.array();
            } else {
                ByteBuffer buff = ByteBuffer.allocate(3);
                buff.put( (byte)
                        ((this.lengthAndaccessMode | this.serviceCodeListOrder) & 0xFF))
                    .put(this.blockNumber[1])
                    .put(this.blockNumber[0]); //little endian
                return buff.array();
            }
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ブロックリストエレメント\n");
            sb.append("  データ長 : " + this.getBytes().length + " byte\n");
            sb.append("  アクセスモード        : " + Util.getBinString((byte)(this.lengthAndaccessMode & 0x8F)) + "\n");
            sb.append("  サービスコードリスト順: " + Util.getHexString(this.serviceCodeListOrder) + "\n");
            sb.append("  ブロックナンバー      : " + Util.getHexString(this.blockNumber) + "\n");
            return sb.toString();
        }   
    }
    
   /**
    * コマンドを実行します
    *
    * <pre>Android 2.3の隠しクラス(@hide)に依存しています。今後の仕様変更で使えなくなるリスクを考慮してください</pre>
    * 
    * @param Tag 隠しクラスである android.nfc.Tag クラスの参照をセットします
    * @param commandPacket 実行するコマンドパケットをセットします
    * @return CommandResponse コマンドの実行結果が戻ります 
    * @throws FeliCaException コマンドの発行に失敗した場合にスローされます
    */
    public static final CommandResponse execute(Parcelable tag, CommandPacket commandPacket) throws FeliCaException {
        byte[] result = executeRaw(tag, commandPacket.getBytes());
        return new CommandResponse(result);
    }
    /**
     * Rawデータを使ってコマンドを実行します
     * 
     * <pre>Android 2.3の隠しクラス(@hide)に依存しています。今後の仕様変更で使えなくなるリスクを考慮してください</pre>
     * 
     * @param Tag 隠しクラスである android.nfc.Tag クラスの参照をセットします
     * @param commandPacket 実行するコマンドパケットをセットします
     * @return byte[] コマンドの実行結果バイト列で戻ります 
     * @throws FeliCaException コマンドの発行に失敗した場合にスローされます
     */
    public static final byte[] executeRaw(Parcelable tag, byte[] commandPacket) throws FeliCaException {
        try {
            NfcAdapter adapter = NfcAdapter.getDefaultAdapter();
            // android.nfc.RawTagConnectionを生成
            Class<?> tagClass = Class.forName("android.nfc.Tag");
            Method createRawTagConnection = 
                adapter.getClass().getMethod("createRawTagConnection", tagClass);
            Object rawTagConnection = createRawTagConnection.invoke(adapter, tag);

            // android.nfc.RawTagConnection#mTagServiceフィールドを取得 (NfcService.INfcTagへの参照が入っている)
            Field f = rawTagConnection.getClass().getDeclaredField("mTagService");
            f.setAccessible(true);
            Object tagService = f.get(rawTagConnection);

            //ServiceHandleを取得
            f = tagClass.getDeclaredField("mServiceHandle");
            f.setAccessible(true);
            int serviceHandle = (Integer) f.get(tag);  
            
            //INfcTag#transceive
            Method transeive = tagService.getClass().getMethod("transceive", Integer.TYPE, byte[].class);

            //Log.d(TAG, "invoking transceive commandPacket :" +  Util.getHexString(commandPacket) + "\n");
            byte[] response = (byte[])transeive.invoke(tagService, serviceHandle, commandPacket);
            if ( response != null ) {
                //Log.d(TAG, "transceive successful. commandResponse = " + Util.getHexString(response) + "\n");
            } else {
                Log.d(TAG, "transceive fail. result null");
                throw new FeliCaException("execute transceive fail" + "\n");
            }
            return response;
        } catch (ClassNotFoundException e){
            throw new FeliCaException(e);
        } catch (NoSuchMethodException e){
            throw new FeliCaException(e);
        } catch (SecurityException e){
            throw new FeliCaException(e);
        } catch (NoSuchFieldException e){
            throw new FeliCaException(e);
        } catch (IllegalAccessException e){
            throw new FeliCaException(e);
        } catch (IllegalArgumentException e){
            throw new FeliCaException(e);
        } catch (InvocationTargetException e){
            throw new FeliCaException(e);
        }
    }
   

}
