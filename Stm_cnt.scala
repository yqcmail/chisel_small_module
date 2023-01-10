import chisel3._ 
import chisel3.util._ 
import chisel3.experimental.{prefix,noPrefix}
import chisel3.stage.ChiselStage
import chisel3.experimental.ChiselEnum




class Stm_cnt(Dw:Int) extends Module with RequireAsyncReset{
  val s_i = IO(Input(UInt(1.W)))  
  val done_o  = IO(Output(UInt(1.W)))  

   val s_idl :: s_mul :: s_div :: s_don :: Nil = Enum(4) 

   val rg_sta  = RegInit(s_idl)
   val div_cnt = RegInit(0.U((log2Up(Dw)+1).W))  //div start 1 to widt: 

    val gt_cnt_dw = div_cnt >= (Dw-1).asUInt 

     val rg_done = RegInit(0.U(1.W))
    //state jump
    switch(rg_sta ){
        is(s_idl) {
          rg_sta := s_mul 
          div_cnt := 0.U 
          rg_done := 0.U 
        }
        is(s_mul) {
          div_cnt := div_cnt + 1.U  
          when(gt_cnt_dw){
             rg_sta := s_div 
             div_cnt := 0.U  
          }
        }
        is(s_div) {
          div_cnt := div_cnt + 1.U  
          when(gt_cnt_dw){
             rg_sta := s_don 
             div_cnt := 0.U  
          }
        }
        is(s_don) {
          rg_sta := s_idl 
          rg_done := 1.U 
        }



    } 
   
    
    //state logic 
   done_o :=  rg_done 

}

//object Main extends App {
//  (new chisel3.stage.ChiselStage)
//    .emitVerilog(new Stm_cnt(Dw=8), 
//     Array(
//        "--target-dir", "../../builds",
//        "--emission-options=disableMemRandomization,disableRegisterRandomization"
//      )
//    )
//
//}
