/**
 * README
 * This extension is being triggered by MMS025MI/AddAlias/PRE
 *
 * Name: ChangeCONO
 * Description: XTend made to copy all the enreg in MITPOP for the POPN in entry in the CONO 0
 *              for after the API can create without unicity problem the POPN in entry
 * Date       Changed By                     Description
 * 20220601   Ludovic TRAVERS                Create CUS_xt_MMS025_A_Chk_Pre_CheckFACI Extension
 * 20221018   Ludovic TRAVERS                Fix post review issues
 * 20231010   François LEPREVOST             Fix post review issues
 */
 
public class ChangeCONO extends ExtendM3Trigger {
  private final ProgramAPI program
  private final DatabaseAPI database
  private final MICallerAPI miCaller
  private final TransactionAPI transaction
  
  int company = 0
  String POPN = ""
  
  public ChangeCONO(ProgramAPI program, DatabaseAPI database, MICallerAPI miCaller, TransactionAPI transaction) {
    this.program = program
    this.database = database
    this.miCaller = miCaller
    this.transaction = transaction
  }
  
  public void main() {
    company = (Integer) program.getLDAZD().CONO
    if (!transaction.parameters.get("CONO").isEmpty()) {
      company = Integer.parseInt(transaction.parameters.get("CONO"))
    }
    
    int alwt = Integer.parseInt(transaction.parameters.get("ALWT"))
    if (alwt == 2 && checkItemExist()) {
      POPN = transaction.parameters.get("POPN")
      
      DBAction query = database.table("MITPOP")
                  .index("20")
                  .selection("MPCONO", "MPITNO", "MPPOPN", "MPLMDT", "MPCHNO", "MPCHID", "MPLVDT", "MPCNQT", "MPALUN", "MPORCO", "MPSEQN", "MPATPE", "MPATNR", "MPCFIN", "MPPRNA",
                              "MPTXID", "MPRGDT", "MPRGTM")
                  .build()
                  
      DBContainer container = query.getContainer()
      container.set("MPCONO", company)
      container.set("MPALWT", 2)
      container.set("MPALWQ", transaction.parameters.get("ALWQ"))
      container.set("MPPOPN", POPN)
      
      Closure<?> releasedItemProcessor = {
        DBContainer data -> 
          addActualsDatsInCono200(data)
          
          Closure<?> deleterCallback = { LockedResult lockedResult ->
            lockedResult.delete()
          }
          
          if (query.read(data)) {
            query.readLock(data, deleterCallback)
          } 
      }
      
      query.readAll(container, 4, releasedItemProcessor)
    }
  }
  
  /**
  * On vérifie si l'ITNO passé en entrée existe afin de ne pas passer les refs complémentaires existantes dans la CONO 200 pour rien.
  */
  private boolean checkItemExist() {
    DBAction query = database.table("MITMAS").index("00").selection("MMITDS").build()
    DBContainer container = query.getContainer()
    container.set("MMCONO", company)
    container.set("MMITNO", transaction.parameters.get("ITNO"))
    
    return query.read(container)
  }

  /**
   * On enregistre les ref complémentaires dans la CONO 200
   */  
  private void addActualsDatsInCono200(DBContainer data) {
    int newCono = 200
    int alwt = 2
    String alwq = transaction.parameters.get("ALWQ")
    String itno = data.get("MPITNO")
    String popn = transaction.parameters.get("POPN")
    String e0pa = transaction.parameters.get("E0PA")
    String sea1 = transaction.parameters.get("SEA1")
    int vfdt = 0
    if (!transaction.parameters.get("VFDT").isEmpty()) {
      vfdt = Integer.parseInt(transaction.parameters.get("VFDT"))
    }
    
    int lvdt = data.get("MPLVDT")
    double cnqt = data.get("MPCNQT")
    String alun = data.get("MPALUN")
    String orco = data.get("MPORCO")
    int seqn = data.get("MPSEQN")
    String remk = data.get("MPREMK")
    String atpe = data.get("MPATPE")
    long atnr = data.get("MPATNR")
    long cfin = data.get("MPCFIN")
    String prna = data.get("MPPRNA")
    long txid = data.get("MPTXID")
    int rgdt = data.get("MPRGDT")
    int rgtm = data.get("MPRGTM")
    int lmdt = data.get("MPLMDT")
    int chno = data.get("MPCHNO")
    String chid = data.get("MPCHID")
    
    DBAction query2 = database.table("MITPOP").index("00").selectAllFields().build()
    DBContainer container2 = query2.getContainer()
    container2.set("MPCONO", newCono)
    container2.set("MPALWT", alwt)
    container2.set("MPALWQ", alwq)
    container2.set("MPITNO", itno)
    container2.set("MPPOPN", popn)
    container2.set("MPE0PA", e0pa)
    container2.set("MPSEA1", sea1)
    container2.set("MPVFDT", vfdt)
    
    if (!query2.read(container2)) {
      container2.set("MPLVDT", lvdt)
      container2.set("MPCNQT", cnqt)
      container2.set("MPALUN", alun)
      container2.set("MPORCO", orco)
      container2.set("MPSEQN", seqn)
      container2.set("MPREMK", remk)
      container2.set("MPATPE", atpe)
      container2.set("MPATNR", atnr)
      container2.set("MPCFIN", cfin)
      container2.set("MPPRNA", prna)
      container2.set("MPTXID", txid)
      container2.set("MPRGDT", rgdt)
      container2.set("MPRGTM", rgtm)
      container2.set("MPLMDT", lmdt)
      container2.set("MPCHNO", chno)
      container2.set("MPCHID", chid)
      
      query2.insert(container2)
    }
  }

}


































