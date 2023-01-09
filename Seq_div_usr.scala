import chisel3._ 
import chisel3.util._ 
import chisel3.experimental.{prefix,noPrefix}
import chisel3.stage.ChiselStage
import chisel3.experimental.ChiselEnum

//
//     a: 0000 1101
//     b: 0010 0000
//    ----------------
//  a<<1: 0001 1010
//     b: 0010 0000
//                      if(a<<1 >b ):0 
//    ----------------
//  a<<1: 0011 0100
//     b: 0010 0000
//                      if(a<<1 >b ):1 
// a-b+1: 0001 0101
//    ----------------
//  a<<1: 0010 1010
//     b: 0010 0000
//                      if(a<<1 >b ):1 
// a-b+1: 0000 1011
//    ----------------
//  a<<1: 0001 0110
//     b: 0010 0000
//                      if(a<<1 >b ):0 
//    ----------------
//           1
//
//
class Seq_div_usr(Dw:Int) extends Module with RequireAsyncReset{
  val s_i = IO(Input(UInt(1.W)))  
  val div_a_i  = IO(Input(UInt(Dw.W)))  
  val div_b_i  = IO(Input(UInt(Dw.W)))  
  val div_q_o = IO(Output(UInt(Dw.W)))  
  val div_q1_o = IO(Output(UInt(Dw.W)))  
  val div_r_o = IO(Output(UInt(Dw.W)))  
  val done_o  = IO(Output(UInt(1.W)))  

   val s_idl :: s_div_s :: s_div_t :: s_div_d ::  Nil = Enum(4) 

   val rg_sta  = RegInit(s_idl)
   val nx_sta  = WireDefault(s_idl)
       rg_sta := nx_sta

  val div_cnt = RegInit(0.U((log2Up(Dw)+1).W))  //div start 1 to widt: 
  
    //state jump
    when(rg_sta === s_idl){
        when(s_i === 1.U){ nx_sta := s_div_s }.otherwise{ nx_sta := s_idl }
    } .elsewhen(rg_sta === s_div_s ){
        when(div_cnt === Dw.asUInt ){ nx_sta := s_div_t }.otherwise{ nx_sta := s_div_s }
    } .elsewhen(rg_sta === s_div_t ){
        nx_sta := s_div_d
    } .elsewhen(rg_sta === s_div_d ){
        nx_sta := s_idl
    } 
   //val ext_a =  (Cat(0.U(Dw.W),div_a_i)).suggestName("wire_ext_a")
   val w_ext_a =  div_a_i
   val ext_b = Cat(div_b_i, 0.U(Dw.W)).asUInt 

   def fun_lt_sub_shift(dat_i:UInt ):UInt ={
     //Mux((dat_i >= ext_b ), ( dat_i - ext_b + 1.U ) << 1, dat_i<<1)
     val lt_ab = (dat_i >= ext_b )
     val tmp = Mux((lt_ab ), ( dat_i - ext_b + 1.U ) , dat_i)
     return tmp<<1
     
   }
 
   val rg_ext_a  = RegInit(0.U((Dw*2+1).W))
   val rg_div_q  = RegInit(0.U(Dw.W))
   val rg_div_q1  = RegInit(0.U(Dw.W))
   val rg_div_r  = RegInit(0.U(Dw.W))   
   val rg_don = RegInit(0.U(1.W))
    //state logic 
    when(nx_sta === s_idl){
      div_cnt := 0.U
      rg_ext_a := div_a_i 
      rg_don := 0.U
      rg_div_q := 0.U 
      rg_div_q1 := 0.U 
    }.elsewhen(nx_sta === s_div_s ) {
        rg_don := 0.U
        when(rg_sta === s_idl){
           div_cnt := 0.U
        }.otherwise{
           div_cnt := div_cnt + 1.U
           rg_ext_a := fun_lt_sub_shift(rg_ext_a)
           rg_div_q := rg_div_q | ((rg_ext_a >= ext_b).asUInt <<(Dw.asUInt -div_cnt)) 
        }
    }.elsewhen(nx_sta === s_div_t){
       rg_ext_a := fun_lt_sub_shift(rg_ext_a)
       rg_div_q := rg_div_q | ((rg_ext_a >= ext_b).asUInt <<(Dw.asUInt -div_cnt)) 
       div_cnt := 0.U
    }.elsewhen(nx_sta === s_div_d){ 
        rg_don := 1.U
        rg_div_q1 := rg_ext_a(Dw,1) 
       rg_div_r := rg_ext_a(Dw*2,Dw+1) 
    }

   div_q_o := rg_div_q
   div_r_o := rg_div_r
   done_o :=  rg_don

   div_q1_o := rg_div_q1 

}

object Main extends App {
  (new chisel3.stage.ChiselStage)
    .emitVerilog(new Seq_div_usr(Dw=8), 
     Array(
        "--target-dir", "../../builds",
        "--emission-options=disableMemRandomization,disableRegisterRandomization"
      )
    )

}



