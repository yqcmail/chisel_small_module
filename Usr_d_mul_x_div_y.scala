import chisel3._ 
import chisel3.util._
import chisel3.experimental.{prefix, noPrefix} 
import chisel3.stage.ChiselStage
import chisel3.experimental.ChiselEnum

//data_i*x_i/y_i
class Usr_d_mul_x_div_y(Dw:Int) extends Module with RequireAsyncReset{
  // override val desiredName = s"Usr_d_mul_x_div_y_$Dw" 
  val start_i = IO(Input (UInt(1.W)))
  val data_i = IO(Input (UInt(Dw.W)))
  val x_i = IO(Input (UInt(Dw.W)))
  val y_i = IO(Input (UInt(Dw.W)))
  val data_o = IO(Output (UInt(Dw.W)))
  val rem_o = IO(Output (UInt(Dw.W)))
  val don_o = IO(Output (UInt(1.W)))

   val s_idl :: s_mul_s :: s_mul_t ::  s_div_s :: s_div_t :: s_end ::  Nil = Enum(6)

   val rg_state = RegInit(s_idl)
   val nx_state = WireDefault(s_idl)
       rg_state := nx_state

   val cnt_s  = RegInit(0.U(log2Up(Dw+2).W))

   val y_not0 = Mux(y_i === 0.U,1.U,y_i)

 //state jump 
    when(rg_state === s_idl){
        when(start_i === 1.U){
         nx_state := s_mul_s
        }.otherwise{
         nx_state := s_idl
        }
    } .elsewhen(rg_state === s_mul_s){
        when( cnt_s >= (Dw-1).asUInt){
         nx_state := s_mul_t
        }.otherwise{
         nx_state := s_mul_s
        }
    } .elsewhen(rg_state === s_mul_t){
         nx_state := s_div_s
    } .elsewhen(rg_state === s_div_s){
        when(cnt_s >= (Dw).asUInt){
          nx_state := s_div_t
        }.otherwise{
          nx_state := s_div_s
        }
    } .elsewhen(rg_state === s_div_t){
       nx_state := s_end
    } .elsewhen(rg_state === s_end){
       nx_state := s_idl
    }
   
  //state logic
  // mul:  data_i*x_i  =  for( mul_acc = mul_acc  +  {if(x_abs[i])? data_i << i : 0   }  ),  if(x_i>0)? mul_acc : - mul_acc
  // div:  ext_div_a = s_mul_s_acc, ext_div_b = {y_i,n},    init( buf_ext_a = ext_div_a,)   run(buf_ext_a = if_lt_sub_shift1) 
  // if_lt_sub_shift1:   if(buf_ext_a >= ext_b) ?  (buf_ext_a - ext_b -1)<<1 :  buf_ext_a<<1 
  // div_q :   div_q(cnt-1) = if(buf_ext_a >= ext_b) ? 1 : 0 
  // div_r :  div_r = buf_ext_a(h_bit)  

  //val mul_acc = RegInit(0.U((Dw*2).W))
  val mul_acc = WireDefault(0.U) 


  def fun_mul_bit():UInt = {
    val mul_bit_shift = Mux(x_i(cnt_s) === 1.U , data_i << cnt_s, 0.U) 
     return  mul_bit_shift
  }

 //val ext_div_a = RegInit(0.U((Dw*2).W))
 val ext_div_a = WireDefault(0.U) 


 val ext_div_b = Cat(y_not0,0.U((Dw).W)) 
 val rg_div_q = RegInit(0.U(Dw.W))
 val rg_div_r = RegInit(0.U(Dw.W))
 val rg_don = RegInit(0.U(1.W))

  val com_add_o = WireDefault(0.U)
  val com_add_a = WireDefault(0.U)
  val com_add_b = WireDefault(0.U)
      com_add_o := com_add_a + com_add_b 

 def fun_lt_sub_shift(data_i:UInt ):UInt ={
     //Mux((data_i >= ext_b ), ( data_i - ext_b + 1.U ) << 1, data_i<<1)
     val lt_ab    = (data_i >= ext_div_b ) 
     val lt_div_b = Mux(lt_ab, (( data_i - ext_div_b) + 1.U)<<1   , data_i<<1)
    // val lt_div_b = Mux((data_i(Dw*2-1,Dw) >= ext_div_b(Dw*2-1,Dw) ), ( com_add_o + 1.U ) , data_i)
      //return   lt_div_b<<1
      return   lt_div_b
   }


  
  val rg_buf_d = RegInit(0.U((Dw*2+1).W))
  when(nx_state === s_idl){ 
      rg_buf_d := mul_acc 
  } .elsewhen(nx_state === s_mul_s){
      rg_buf_d := mul_acc 
  } .elsewhen(nx_state === s_mul_t){
      rg_buf_d := mul_acc 
  } .elsewhen(nx_state === s_div_s){
      rg_buf_d := ext_div_a 
  } .elsewhen(nx_state === s_div_t){ 
      rg_buf_d := ext_div_a 
  }
      
  when(nx_state === s_idl){
        cnt_s := 0.U
        mul_acc := 0.U
        ext_div_a := 0.U
        rg_don := 0.U
        com_add_a := 0.U
        com_add_b := 0.U
    } .elsewhen(nx_state === s_mul_s){


        when(rg_state === s_idl){
          cnt_s := 0.U
          mul_acc := 0.U
        }.otherwise{
           cnt_s := cnt_s + 1.U
           //mul_acc := mul_acc + fun_mul_bit()
           mul_acc := rg_buf_d + fun_mul_bit()
        
           //com_add_a := rg_buf_d
           //com_add_b := fun_mul_bit()
           //mul_acc := com_add_o 
        } 
    } .elsewhen(nx_state === s_mul_t){
           mul_acc := rg_buf_d + fun_mul_bit()
    } .elsewhen(nx_state === s_div_s){
        when(rg_state === s_mul_t){
          cnt_s := 0.U
          //ext_div_a := mul_acc 
          ext_div_a := rg_buf_d 
        }.otherwise{
            cnt_s := cnt_s + 1.U
            //ext_div_a := fun_lt_sub_shift(ext_div_a) 
            ext_div_a := fun_lt_sub_shift(rg_buf_d) 
           
           //com_add_a := rg_buf_d 
           //com_add_b := -ext_div_b
        } 
    
    } .elsewhen(nx_state === s_div_t){ 
        //rg_div_q := ext_div_a(Dw-1,0)
        //rg_div_r := ext_div_a(Dw*2-1,Dw)             
        ext_div_a := fun_lt_sub_shift(rg_buf_d) 
        
    } .elsewhen(nx_state === s_end){ 
        rg_div_q := rg_buf_d(Dw,1)
        rg_div_r := rg_buf_d(Dw*2,Dw+1)             
        rg_don := 1.U
    }


  //////////////////////  
   data_o := rg_div_q 
   don_o := rg_don 
   rem_o := rg_div_r  
}

object Main extends App {
  (new chisel3.stage.ChiselStage)
    .emitVerilog(new Usr_d_mul_x_div_y(Dw=8), 
     Array(
        "--target-dir", "../../builds",
        "-e", "verilog",
        "--emission-options=disableMemRandomization,disableRegisterRandomization"
      )
    )

}


