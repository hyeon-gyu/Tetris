
package com.example.chat.controller

import com.example.chat.controller.ChatController.Companion.clientTetrisMap
import com.example.chat.model.CTetris
import com.example.chat.model.ChatMessage
import com.example.chat.model.Tetris
import com.example.chat.model.TetrisState
import com.example.chat.model.blockarray.setOfBlockArrays
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.stereotype.Controller
import java.util.*


@Controller
class ChatController {

    companion object{
        var random = Random()
        var clientTetrisMap:MutableMap<String, CTetris> = HashMap()
        var oneTimeUseMap:MutableMap<String, String> = HashMap()
        var member_count: Int = 0
        var reset_Cnt:Int = 0

        private const val MAX_MEMBER_COUNT = 3  /** 접속인원 3명으로 제한*/

    }

    @MessageMapping("/chat.register") //login 창
    @SendTo("/topic/prevuser")
    fun register(@Payload chatMessage: ChatMessage, headerAccessor: SimpMessageHeaderAccessor): ChatMessage {
        headerAccessor.sessionAttributes!!["username"] = chatMessage.sender
        Tetris.init(setOfBlockArrays)
        var board = CTetris(15,10)
        board.state = TetrisState.NewBlock
        var initKey= chatMessage.idxBT
        println("random num is $initKey")
        board.state = board.accept(initKey!!)
        board.printScreen()
        oneTimeUseMap[chatMessage.sender!!] = initKey //초기 생성 랜덤 숫자를 저장, ctetris 객체를 저장하지말고
        clientTetrisMap[chatMessage.sender!!] = board //hashmap에 board instance 저장
        chatMessage.oneTimeUseMap = oneTimeUseMap
        member_count ++

        if(member_count == MAX_MEMBER_COUNT)
            chatMessage.readyOrStart = "Start"
        return chatMessage
    }

    @MessageMapping("/chat.send")
    @SendTo("/topic/public")
    fun sendMessage(@Payload chatMessage: ChatMessage): ChatMessage {
        var sender = chatMessage.sender //보낸사람 확인
        var board = clientTetrisMap[sender] //hash map에서 user name찾아서 board객체 찾아오기
        when(board!!.state){
            TetrisState.Finished -> {
                print(" $sender board game over")
                clientTetrisMap[chatMessage.sender!!] = board
                chatMessage.alert = "finished"
                return chatMessage
            }
            TetrisState.Running -> {
                if(chatMessage.key == "q"){
                    return handleQuit(board,chatMessage)
                }
                else{
                    return handleRunning(board,chatMessage)
                }
            }
            TetrisState.NewBlock -> {
                return handleNewBlock(board,chatMessage)
            }
            else -> {
                println("wrong key")
                chatMessage.content = "wrong key"
                return chatMessage
            }
        }

    }

    @MessageMapping("/chat.reset")
    @SendTo("/topic/resetgame")
    fun increaseCnt():ChatMessage?{
        reset_Cnt++
        var chatMessage:ChatMessage = ChatMessage()
        if(reset_Cnt== MAX_MEMBER_COUNT){
            clientTetrisMap = HashMap()
            oneTimeUseMap = HashMap()
            member_count=0
            reset_Cnt=0
            chatMessage.resetGame=true
            return chatMessage
        }
        return chatMessage //null로 보내면 아무동작도 안함, 왔던 message 그대로 담아서 보내주자 일단은..
    }
}

private fun handleQuit(board:CTetris,chatMessage: ChatMessage): ChatMessage {
    board.printScreen()
    println("current board id: $board")
    board.state = TetrisState.Finished
    chatMessage.alert = "game quit"
    clientTetrisMap[chatMessage.sender!!] = board
    println(chatMessage)
    /** chatmessage 구조
     * content : 'q'
     * key : 'q'
     * sender: 'q'를 입력한 사용자
     * idxBT : NULL
     * alert : "game quit" */

    return chatMessage
}

private fun handleRunning(board:CTetris, chatMessage: ChatMessage): ChatMessage {
    board.state = board.accept(chatMessage.key!!)
    board.printScreen()
    if (chatMessage.idxBT != null && board.state == TetrisState.NewBlock) { //newblock일때 동작
        return handleNewBlock(board, chatMessage)
    }
    clientTetrisMap[chatMessage.sender!!] = board
    return chatMessage
}

fun handleNewBlock(board: CTetris,chatMessage: ChatMessage):ChatMessage{
    board.state = board.accept(chatMessage.idxBT!!)
    board.printScreen()
    println()
    if(board.state == TetrisState.Finished){
        chatMessage.alert = "finished"
        clientTetrisMap[chatMessage.sender!!] = board
        return chatMessage
    }
    clientTetrisMap[chatMessage.sender!!] = board
    return chatMessage
}