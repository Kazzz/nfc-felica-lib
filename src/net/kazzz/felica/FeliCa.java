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
package net.kazzz.felica;

import static net.kazzz.felica.lib.FeliCaLib.COMMAND_POLLING;
import static net.kazzz.felica.lib.FeliCaLib.COMMAND_READ_WO_ENCRYPTION;
import static net.kazzz.felica.lib.FeliCaLib.COMMAND_WRITE_WO_ENCRYPTION;

import java.nio.ByteBuffer;

import net.kazzz.felica.command.PollingResponse;
import net.kazzz.felica.command.ReadResponse;
import net.kazzz.felica.lib.FeliCaLib;
import net.kazzz.felica.lib.FeliCaLib.CommandPacket;
import net.kazzz.felica.lib.FeliCaLib.CommandResponse;
import net.kazzz.felica.lib.FeliCaLib.IDm;
import net.kazzz.felica.lib.FeliCaLib.PMm;
import android.os.Parcelable;

/**
 * IFeliCaのデフォルト実装クラスを提供します
 * 
 * @author Kazzz
 * @date 2011/01/23
 * @since Android API Level 9
 *
 */

public class FeliCa implements IFeriCa {
     // システムコード
    public static final int SYSTEMCODE_ANY = 0xffff;       // ANY
    public static final int SYSTEMCODE_COMMON = 0xfe00;    // 共通領域
    public static final int SYSTEMCODE_CYBERNE = 0x0003;   // サイバネ領域
    public static final int SYSTEMCODE_EDY = 0xfe00;       // Edy (=共通領域)
    public static final int SYSTEMCODE_SUICA = 0x0003;     // Suica (=サイバネ領域)
    public static final int SYSTEMCODE_PASMO = 0x0003;     // Pasmo (=サイバネ領域)
    
    // サービスコード suica/pasmo (little endian)
    public static final int SERVICE_SUICA_INOUT = 0x108f;   // SUICA/PASMO 入退場記録
    public static final int SERVICE_SUICA_HISTORY = 0x090f; // SUICA/PASMO履歴
    
    public static final int STATUSFLAG1_NORMAL = 0x00; //正常終了 
    public static final int STATUSFLAG1_ERROR = 0xff;  //エラー　(ブロック番号に依らない)

    public static final int STATUSFLAG2_NORMAL = 0x00;          //正常終了
    public static final int STATUSFLAG2_ERROR_LENGTH    = 0x01; 
    public static final int STATUSFLAG2_ERROR_FLOWN     = 0x02; 
    public static final int STATUSFLAG2_ERROR_MEMORY    = 0x70; 
    public static final int STATUSFLAG2_ERROR_WRITELIMIT= 0x71; 

    protected final Parcelable tagService;
    protected IDm idm;
    protected PMm pmm;
    /**
     * コンストラクタ
     * 
     * @param tagService NFCTagサービスへの参照をセット
     */
    public FeliCa(Parcelable tagService) {
        this.tagService =  tagService;
    }
    /* (non-Javadoc)
     * @see net.kazzz.felica.IFeriCa#polling(short)
     */
    @Override
    public void polling(int systemCode) throws FeliCaException {
        if ( this.tagService == null ) {
            throw new FeliCaException("tagService is null. no polling execution");
        }
        CommandPacket polling = 
            new CommandPacket(COMMAND_POLLING
                    , new byte[] {
                      (byte) (systemCode >> 8)  // システムコード
                    , (byte) (systemCode & 0xff)
                    , (byte) 0x01              //　システムコードリクエスト
                    , (byte) 0x00});           // タイムスロット}; 
        CommandResponse r = FeliCaLib.execute(this.tagService, polling);
        PollingResponse pr = new PollingResponse(r);
        this.idm = pr.getIDm();
        this.pmm = pr.getPMm();
    }
    /* (non-Javadoc)
     * @see net.kazzz.felica.IFeriCa#getIDm()
     */
    @Override
    public IDm getIDm() throws FeliCaException {
        return this.idm;
    }
    /* (non-Javadoc)
     * @see net.kazzz.felica.IFeriCa#getPMm()
     */
    @Override
    public PMm getPMm() throws FeliCaException {
        return this.pmm;
    }
    /* (non-Javadoc)
     * @see net.kazzz.felica.IFeriCa#readWithoutEncryption(short, byte)
     */
    @Override
    public byte[] readWithoutEncryption(int serviceCode,
            byte addr) throws FeliCaException {
        if ( this.tagService == null ) {
            throw new FeliCaException("tagService is null. no read execution");
        }
        // read without encryption
        CommandPacket readWoEncrypt = 
            new CommandPacket(COMMAND_READ_WO_ENCRYPTION, idm
                ,  new byte[]{(byte) 0x01         // サービス数
                    , (byte) (serviceCode & 0xff) // サービスコード (little endian)
                    , (byte) (serviceCode >> 8)
                    , (byte) 0x01                 // 同時読み込みブロック数
                    , (byte) 0x80, addr });       // ブロックリスト
        CommandResponse r = FeliCaLib.execute(this.tagService, readWoEncrypt);
        ReadResponse rr = new ReadResponse(r); 
        
        if ( rr.getStatusFlag1() == 0 ) {
            return rr.getBlockData();
        } else {
            return null; //error
        }
    }
    
    
    /* (non-Javadoc)
     * @see net.kazzz.felica.IFeriCa#writeWWithoutEncryption(short, byte, byte[])
     */
    @Override
    public int writeWithoutEncryption(int serviceCode,
            byte addr, byte[] buff) throws FeliCaException {
        if ( this.tagService == null ) {
            throw new FeliCaException("tagService is null. no write execution");
        }
        // write without encryption
        ByteBuffer b =  ByteBuffer.allocate(6 + buff.length);
        b.put(new byte[]{(byte) 0x01          // Number of Service
                , (byte) (serviceCode & 0xff) // サービスコード (little endian)
                , (byte) (serviceCode >> 8)
                , (byte) buff.length          // 同時書き込みブロック数
                , (byte) 0x80, (byte) addr    // ブロックリスト
                });
        b.put(buff); //書き出すデータ
        
        CommandPacket writeWoEncrypt = 
            new CommandPacket(COMMAND_WRITE_WO_ENCRYPTION, idm, b.array());
        CommandResponse r = FeliCaLib.execute(this.tagService, writeWoEncrypt);
        return r.getBytes()[9] == 0 ? 0 : -1;
    }
    
}
