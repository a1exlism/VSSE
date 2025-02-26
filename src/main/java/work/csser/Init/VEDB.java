package work.csser.Init;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

import work.csser.db.KeywordPairSet;
import work.csser.db.TSet;
import work.csser.utils.Common;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author a1exlism
 * @className VEDB
 * @description Verifiable Encrypted DataBase SYSTEM
 * @since 2019/12/7 20:34
 */
public class VEDB {
  final static private Pairing pairing = PairingFactory.getPairing("params/curves/a.properties");
  private static MasterKey MK = MasterKey.readKey();
  private static PublicKey PK = PublicKey.readKey();

  /**
   * Algorithm 1_for loop
   *
   * @param keyword:
   * @param filenames:
   * @return KeywordPairSet[TSets, XSets, stagw]
   * @method setup
   * @params [keyword, filenames]
   */
  public static KeywordPairSet setup(String keyword, ArrayList<String> filenames) throws Exception {
    ArrayList<TSet> TSets = new ArrayList<>();
    ArrayList<String> XSets = new ArrayList<>();
    byte[] Ke = Common.PRF_F(MK.getKs(), keyword);

    Element w = pairing.getZr().newElement();
    w.setFromBytes(keyword.getBytes(StandardCharsets.UTF_8));
    Element wInv = w.duplicate().invert().getImmutable();
    byte[] stagw = Common.PRF_F(MK.getKs(), MK.getG1().getElement().duplicate().powZn(wInv));

    int c = 0;
    for (String ind : filenames) { //  ind - filename
      Element xInd = Common.PRF_Fp(MK.getKi(), ind);
      Element z = Common.PRF_Fp(MK.getKz(),
          MK.getG2().getElement().duplicate().powZn(wInv).toString() + c);
      Element zInv = z.duplicate().invert();
      byte[] l = Common.PRF_F(stagw, c + "");
      byte[] e = Common.Enc(Ke, ind);
      Element y = xInd.duplicate().mul(zInv);

      TSet tSet = new TSet(Arrays.toString(l), e, y, ind);
      TSets.add(tSet);

      Element g = PK.getG().getElement().getImmutable();
      Element g3 = MK.getG3().getElement().getImmutable();
      Element xtagw_exp = Common.PRF_Fp(MK.getKx(),
          g3.powZn(wInv.duplicate()).toString()).mul(xInd.duplicate());
      Element xtagw = g.powZn(xtagw_exp);
      XSets.add(xtagw.toString());

      ++c;
    }
    return new KeywordPairSet(TSets, XSets, stagw);
  }
}
