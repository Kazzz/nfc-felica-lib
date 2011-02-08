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

import net.kazzz.felica.lib.FeliCaLib.IDm;
import net.kazzz.felica.lib.FeliCaLib.PMm;

/**
 * FeliCa 操作を抽象化したインタフェースを提供します
 * 
 * @author Kazzz
 * @date 2011/01/23
 * @since Android API Level 9
 *
 */

public interface IFeriCa {
    /**
     * FeliCa をポーリングする
     * @param systemCode 対象システムコードをセット
     * @return
     */
    void polling(int systemCode) throws FeliCaException;
    /**
     * IDmを取得します
     * @return PICCを識別するIDmを取得します
     * @throws FeliCaException 
     */
    IDm getIDm() throws FeliCaException;
    /**
     * PMmを取得する
     * @return PMmが戻ります
     * @throws FeliCaException
     */
    PMm getPMm() throws FeliCaException;
    /**
     * FeliCaからデータを読み込みます(認証不要な非暗号化版)
     * 
     * @param serviceCode  サービスコードをセット (0x090f等)
     * @param addr アドレス (ブロック番号)をセット
     * @return byte[] 読み込んだブロックの内容が戻ります
     * @throws FeliCaException
     */
    byte[] readWithoutEncryption(int serviceCode, byte addr) throws FeliCaException;
    /**
     * FeliCa側にデータを書き込みます(認証不要な非暗号化版)
     *  
     * @param serviceCode  サービスコードをセット 
     * @param addr 書きこむアドレス(ブロック番号をセット
     * @param buff 書きこむブロックのデータをセット
     * @return int エラーステータスが戻ります (0:正常終了 非0:異常終了)
     * @throws FeliCaException
     */
    int writeWithoutEncryption(int serviceCode, byte addr, final byte[] buff) throws FeliCaException; 
}
