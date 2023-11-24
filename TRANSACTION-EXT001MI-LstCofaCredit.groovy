/**
 * README
 * API spécifique afin de croiser OCUSMA, FSLEDG et FGLEDG pour les interfaces COFA Crédit
 *
 * Name: LstCofaCredit
 * Description: 
 * Date       Changed By                     Description
 * 20231019   François Leprévost             Création verbe LstCofaCredit
 */
public class LstCofaCredit extends ExtendM3Transaction {
  private final MIAPI mi
  private final DatabaseAPI database
  private final ProgramAPI program
  private final UtilityAPI utility
  
  public LstCofaCredit(MIAPI mi, DatabaseAPI database , ProgramAPI program, MICallerAPI miCaller, UtilityAPI utility) {
    this.mi = mi
    this.database = database
    this.program = program
    this.utility = utility
  }
  
  int cono = 0
  String divi = ""
  String cfc8 = ""
  String cucd = ""
  int isfr = 0
  int fc10 = 0
  
  public void main() {
    cono = (Integer) program.getLDAZD().CONO
    divi = mi.inData.get("DIVI").trim()
    String isfrRecu = mi.inData.get("ISFR").trim()
    cfc8 = mi.inData.get("CFC8").trim()
    cucd = mi.inData.get("CUCD").trim()
    String fc10Recu = mi.inData.get("FC10").trim()
    
    if (divi.isEmpty()) {
      mi.error("La division est obligatoire.")
      return
    } else if (!checkDiviExist()) {
      mi.error("La division est inexistante.")
      return
    }
    
    if (isfrRecu.isEmpty()) {
      mi.error("Le champ ISFR est obligatoire.")
      return
    } else if (!isfrRecu.equals("1") && !isfrRecu.equals("0")) {
      mi.error("Le champ ISFR accepte uniquement les valeurs 0 ou 1.")
      return
    } else {
      isfr = Integer.parseInt(isfrRecu)
    }
    
    if (fc10Recu.isEmpty()) {
      mi.error("Le champ FC10 est obligatoire.")
      return
    } else if (!fc10Recu.equals("1") && !fc10Recu.equals("0")) {
      mi.error("Le champ FC10 accepte uniquement les valeurs 0 ou 1.")
      return
    } else {
      fc10 = Integer.parseInt(fc10Recu)
    }
    
    searchCustomers()
    
  }
  
  // On vérifie que la DIVI existe
  private boolean checkDiviExist() {
    DBAction query = database.table("CMNDIV").index("00").selection("CCCONM").build()
    DBContainer container = query.getContainer()
    container.set("CCCONO", cono)
    container.set("CCDIVI", divi)
    
    return query.read(container)
  }
  
  // Recherche de la liste des clients en filtrant sur CFC8 et CSCD.
  private void searchCustomers() {
    DBAction query = database.table("OCUSMA").index("00").selection("OKCUNO", "OKCFC8", "OKCSCD").build()
    DBContainer container = query.getContainer()
    container.set("OKCONO", cono)
    
    Closure<?> releasedCustomerProcessor = {
      DBContainer data ->
      
      String CUNO = String.valueOf(data.get("OKCUNO")).trim()
      String CFC8 = String.valueOf(data.get("OKCFC8")).trim()
      String CSCD = String.valueOf(data.get("OKCSCD")).trim()
      
      if (CFC8.equals(cfc8)) {
        if ((isfr == 1 && CSCD.equals("FR")) || (isfr == 0 && !CSCD.equals("FR"))) {
          searchFsledgEnregs(CUNO)
        }
      }
    }
     
    int pageSize = mi.getMaxRecords() <= 0 || mi.getMaxRecords() >= 10000 ? 10000: mi.getMaxRecords()
    query.readAll(container, 1, pageSize, releasedCustomerProcessor)
  }
  
  // Recherche des factures dans FSLEDG pour les clients trouvés précédemment.
  private searchFsledgEnregs(String cuno) {
    DBAction query = database.table("FSLEDG")
                             .index("40")
                             .selectAllFields()
                             .build()
    DBContainer container = query.getContainer()
    container.set("ESCONO", cono)
    container.set("ESDIVI", divi)
    container.set("ESCUNO", cuno)
                             
    Closure<?> releasedFsledgProcessor = {
      DBContainer data ->
      double cuam = (double) data.get("ESCUAM")
      int iicd = (int) data.get("ESIICD")
      int trcd = (int) data.get("ESTRCD")
      String cucdFsledg = String.valueOf(data.get("ESCUCD")).trim()
      
      if (cuam != 0d && iicd != 9 && cucd.equals(cucdFsledg)) {
        if ((fc10 == 1 && trcd == 10) || (fc10 == 0 && trcd != 10)) {
          searchFgledg(data, cuno)
        }
      }
    }
    
    int pageSize = mi.getMaxRecords() <= 0 || mi.getMaxRecords() >= 10000 ? 10000: mi.getMaxRecords()
    query.readAll(container, 3, pageSize, releasedFsledgProcessor)
  }
  
  // recherche du VTXT dans FGLEDG
  private searchFgledg(DBContainer dataFsledg, String cuno) {
    int jrno = (int) dataFsledg.get("ESJRNO")
    int jsno = (int) dataFsledg.get("ESJSNO")
    int yea4 = (int) dataFsledg.get("ESYEA4") 
    
    DBAction query = database.table("FGLEDG").index("00").selection("EGVTXT").build()
    DBContainer container = query.getContainer()
    container.set("EGCONO", cono)
    container.set("EGDIVI", divi)
    container.set("EGYEA4", yea4)
    container.set("EGJRNO", jrno)
    container.set("EGJSNO", jsno)
    
    Closure<?> releasedFsledgProcessor = {
      DBContainer data ->
      
      mi.outData.put("VTXT", String.valueOf(data.get("EGVTXT")))
    }
    
    query.readAll(container, 5, releasedFsledgProcessor)
    
    mi.outData.put("CUNO", cuno)
    mi.outData.put("YEA4", String.valueOf(yea4))
    mi.outData.put("JRNO", String.valueOf(jrno))
    mi.outData.put("JSNO", String.valueOf(jsno))
    mi.outData.put("PYNO", String.valueOf(dataFsledg.get("ESPYNO")))
    mi.outData.put("IVDT", String.valueOf(dataFsledg.get("ESIVDT")))
    mi.outData.put("DUDT", String.valueOf(dataFsledg.get("ESDUDT")))
    mi.outData.put("RMBL", String.valueOf(dataFsledg.get("ESRMBL")))
    mi.outData.put("PYCD", String.valueOf(dataFsledg.get("ESPYCD")))
    mi.outData.put("CUCD", String.valueOf(dataFsledg.get("ESCUCD")))
    double cuam = (double) dataFsledg.get("ESCUAM")
    mi.outData.put("CUAM", String.valueOf(cuam))
    mi.outData.put("PYRS", String.valueOf(dataFsledg.get("ESPYRS")))
    mi.outData.put("RMST", String.valueOf(dataFsledg.get("ESRMST")))
    mi.outData.put("TEPY", String.valueOf(dataFsledg.get("ESTEPY")))
    mi.outData.put("CINO", String.valueOf(dataFsledg.get("ESCINO")))
    if (cuam >= 0) {
      mi.outData.put("SENS", "C")
    } else {
      mi.outData.put("SENS", "D")
    }
    mi.write()
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
}