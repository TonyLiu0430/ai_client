package com.example.computer_network_hw_app

import android.app.Activity
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import com.example.computer_network_hw_app.ui.theme.Computer_network_hw_appTheme
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Dp
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.window.layout.WindowMetricsCalculator
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.max

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Computer_network_hw_appTheme {
                val navController = rememberNavController();
                NavHost(navController = navController, startDestination = "ChatScreen") {
                    composable("ChatScreen") {
                        ChatScreen(navController)
                    }
                    composable("SettingScreen") {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text("設定") },
                                    navigationIcon = {
                                        IconButton(onClick = {
                                            navController.popBackStack()
                                        }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "return")
                                        }
                                    }
                                )
                            }
                        ) { paddingValues ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                SettingScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavHostController) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerChatHistory()
            }
        },
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Chat") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "開啟側邊欄")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                navController.navigate("SettingScreen")
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = "Setting")
                            }
                        }
                    )
                },
                bottomBar = {
                    // 將 MessageInputField 放在 bottomBar 中
                    MessageInputField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding())
                    )
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    ChatView(modifier = Modifier
                        .fillMaxWidth())
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatView(modifier : Modifier, viewModel: ChatViewModel = hiltViewModel()) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    val scrollState by viewModel.scrollState.collectAsState()

    val view = LocalView.current
    val imeBottomPx = ViewCompat.getRootWindowInsets(view)?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
    LaunchedEffect(WindowInsets.isImeVisible) {
        val originValue = scrollState.value
        for (i in 0..200) {
            if(imeBottomPx > 0) {
                scrollState.animateScrollTo(originValue + imeBottomPx)
            }
            delay(1)
        }
    }

    val widthPixel = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(LocalContext.current).bounds.width()
    val screenWidthDp = with(LocalDensity.current) { widthPixel.toDp() }



    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        chatMessages.forEach { chatMessage ->
            if (chatMessage.sender == Sender.USER) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.padding(4.dp).widthIn(max = screenWidthDp * 0.65f),
                    ) {
                        Text(
                            text = chatMessage.message,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            } else { // BOT or RECEIVING
                var expanded by remember { mutableStateOf(false) }
                val receiving by viewModel.receiving.collectAsState()
                var enableSelect by remember { mutableStateOf(false) }
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .clickable {
                            if (chatMessage.sender == Sender.BOT && !receiving /*TODO*/) {
                                expanded = true
                            }
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (enableSelect) {
                        SelectionContainer {
                            Text(
                                text = chatMessage.message,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = modifier.padding(8.dp),
                                fontSize = if (chatMessage.sender == Sender.BOT) TextUnit.Unspecified else 30.sp,
                            )
                        }
                    }
                    else {
                        Text(
                            text = chatMessage.message,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = modifier.padding(8.dp),
                            fontSize = if (chatMessage.sender == Sender.BOT) TextUnit.Unspecified else 30.sp,
                        )
                    }


                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                if (enableSelect) {
                                    Text("取消選取")
                                }
                                else {
                                    Text("選取內容")
                                }
                            },
                            onClick = {
                                enableSelect = true
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("語音輸出") },
                            onClick = {
                                viewModel.tts.speech(chatMessage.message)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageInputField(modifier: Modifier = Modifier, viewModel: ChatViewModel = hiltViewModel()) {
    var text by remember { mutableStateOf("") }
    val serverConnected by viewModel.serverConnected.collectAsState().also { println("serverConnected: $it") }
    val receiving by viewModel.receiving.collectAsState()
    val inputHint = if (serverConnected) "輸入訊息" else "尚未連接伺服器"
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!results.isNullOrEmpty()) {
                    val recognizedText = results[0]
                    viewModel.chat(recognizedText)
                }
            }
        }
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(inputHint) },
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.small,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = Color(0xFF1E1E1E),
                unfocusedContainerColor = Color(0xFF1E1E1E),
            ),
        )
        if (text.isBlank()) {
            // 語音識別
            IconButton(
                enabled = serverConnected && !receiving,
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "說話啊")
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

                    speechRecognizerLauncher.launch(intent)
                }
            ) {
                //<a href="https://www.flaticon.com/free-icons/radio" title="radio icons">Radio icons created by Freepik - Flaticon</a>
                Icon(painter = painterResource(id = R.drawable.wave_sound), contentDescription = "Send")
            }
        }
        else {
            IconButton(
                enabled = serverConnected && !receiving && text.isNotBlank(),
                onClick = {
                    viewModel.chat(text)
                    text = ""
                }
            ) {
                Icon(painter = painterResource(id = R.drawable.step_out_24), contentDescription = "Send")
            }
        }
    }
}

@Composable
fun DrawerChatHistory(viewModel: ChatHistoryViewModel = hiltViewModel(), chatViewModel: ChatViewModel = hiltViewModel()) {
    val chatHistory by viewModel.chatHistory.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .fillMaxHeight()
            .height(200.dp)
            .padding(16.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(text = "歷史紀錄", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {
                        chatViewModel.startNewChat()
                    }
                ) {
                    Icon(painter = painterResource(id = R.drawable.edit_square_24), contentDescription = "Send")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                chatHistory.forEach {
                    Spacer(Modifier.height(10.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { chatViewModel.changeChatId(it.id) },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                    ) {
                        Text(
                            text = it.title,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            textAlign = TextAlign.Start,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        var showDialog by remember { mutableStateOf(false) }

        SettingsTextButton(
            onClick = { showDialog = true },
            title = "設定 server IP",
            description = "設定 ip 格式 ip:port",
            currentValue = viewModel.getSetting("SERVER_IP") ?: ""
        )

        SettingsTextButton(
            title = "icon provider",
            description = "https://www.flaticon.com/free-icons/radio",
            currentValue = "",
            onClick = {}
        )

        if (showDialog) {
            val serverIp = viewModel.getSetting("SERVER_IP") ?: ""
            var text by remember { mutableStateOf(serverIp) }
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("SERVER_IP") },
                text = {
                    TextField(value = text, onValueChange = { text = it }, label = { Text("0.0.0.0:80") })
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.setSetting("SERVER_IP", text)
                        showDialog = false
                    }) {
                        Text("確定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDialog = false;
                    }) {
                        Text("返回")
                    }
                }
            )
        }


    }
}

@Composable
fun SettingsTextButton(
    title: String,
    description: String,
    currentValue: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)

    ) {
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ){
            Text(text = description, fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = currentValue, fontSize = 14.sp, color = Color.Gray)
        }

    }
}
