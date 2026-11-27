package com.example.fastcash.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fastcash.data.remote.*
import com.example.fastcash.ui.viewmodel.FastCashViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun AgentDesk(vm: FastCashViewModel) {
    if (!vm.isAgent) return
    val api = vm.agentApi
    val scope = rememberCoroutineScope()
    val accounts by vm.activeAgentAccounts.collectAsState()
    var ref by remember { mutableStateOf("") }
    var row by remember { mutableStateOf<AgentReview?>(null) }
    var reason by remember { mutableStateOf("") }
    var decision by remember { mutableStateOf<String?>(null) }
    var bank by remember { mutableStateOf("") }
    var holder by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    fun run(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        feedback = ""
        scope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { feedback = "ක්‍රියාව තහවුරු නොවීය. තොරතුරු නැවත ලබා බලන්න." }
            finally { busy = false }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("නියෝජිත සේවය", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(ref, { ref = it; row = null; reason = "" }, enabled = !busy,
            label = { Text("WhatsApp පණිවිඩයේ TXN යොමු අංකය") }, singleLine = true)
        Button(onClick = { run {
            val result = api.review(ref.trim())
            row = null
            if (result.code() == 404) feedback = "ඉල්ලීම හමු නොවීය."
            else { check(result.isSuccessful); row = requireNotNull(result.body()) }
        } }, enabled = !busy && ref.isNotBlank()) { Text("ඉල්ලීම බලන්න") }
        row?.let { tx ->
            Text("${tx.type} · LKR ${tx.amount} · ${tx.status}")
            Text("Player ID: ${tx.playerId}\n${tx.reference}")
            Text(listOfNotNull(tx.paymentMethod, tx.destination, tx.receiptKey, tx.bank,
                tx.accountNumber, tx.securityCode?.let { "Withdrawal code: $it" }).joinToString("\n"))
            if (tx.status in listOf("PENDING", "PROCESSING")) {
                OutlinedTextField(reason, {reason = it.take(300)}, label = { Text("ප්‍රතික්ෂේප කිරීමට හේතුව") })
                Button(onClick = { decision = "COMPLETED" }, enabled = !busy) {
                    Text(if (tx.type == "DEPOSIT") "MobCash තැන්පතුව අවසන් කළා" else "Playerට මුදල් ගෙව්වා")
                }
                OutlinedButton(onClick = { decision = "REJECTED" }, enabled = !busy && reason.isNotBlank()) {
                    Text("ප්‍රතික්ෂේප කරන්න")
                }
            }
        }
        if (feedback.isNotBlank()) Text(feedback)
        HorizontalDivider()
        Text("Deposit සඳහා බැංකු ගිණුම්", style = MaterialTheme.typography.titleLarge)
        accounts.forEach { account ->
            Text("${account.label}\n${account.accountNumber}")
            TextButton(onClick = { run {
                check(api.disableAccount(account.id).isSuccessful)
                vm.refreshOnce()
            } }, enabled = !busy) { Text("Deposit තිරයෙන් සඟවන්න") }
        }
        OutlinedTextField(bank, { bank = it }, label = { Text("බැංකුව") }, singleLine = true)
        OutlinedTextField(holder, { holder = it }, label = { Text("ගිණුම් හිමියාගේ නම") }, singleLine = true)
        OutlinedTextField(number, { number = it }, label = { Text("ගිණුම් අංකය") }, singleLine = true)
        Button(onClick = { run {
            check(api.publishAccount(AgentBankInput(bank.trim(), holder.trim(), number.trim())).isSuccessful)
            bank = ""; holder = ""; number = ""; vm.refreshOnce()
            feedback = "ගිණුම පාරිභෝගිකයන්ට පෙන්වීමට සුරැකුණා."
        } }, enabled = !busy && bank.isNotBlank() && holder.isNotBlank() && number.matches(Regex("[0-9]{6,30}"))) {
            Text("පාරිභෝගිකයන්ට පෙන්වන්න")
        }
    }
    val status = decision
    val tx = row
    if (status != null && tx != null) AlertDialog(onDismissRequest = { decision = null },
        title = { Text("Playerට දැනුම් දෙන්නද?") },
        text = { Text("${tx.reference}\nPlayer ${tx.playerId} · LKR ${tx.amount}\n$status") },
        confirmButton = { TextButton(onClick = {
            decision = null
            run {
                val result = api.decide(tx.reference, AdminUpdateStatusRequest(status, reason.trim()))
                if (result.code() == 409) feedback = "මෙම ඉල්ලීමට දැනටමත් තීරණයක් ලබා දී ඇත."
                else { check(result.isSuccessful); feedback = "Playerගේ දැනුම්දීම්වලට තීරණය එක් කළා." }
                row = null
                val updated = api.review(tx.reference)
                if (updated.isSuccessful) row = updated.body()
            }
        }) { Text("තහවුරු කරන්න") } },
        dismissButton = { TextButton(onClick = {decision = null}) { Text("ආපසු") } })
}
