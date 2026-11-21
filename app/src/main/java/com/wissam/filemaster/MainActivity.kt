package com.wissam.filemaster

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.wissam.filemaster.ui.asPainter
import java.io.File
import java.text.DateFormat
import java.util.Date

private val Navy=Color(0xFF070D18); private val Surface=Color(0xFF101B2D); private val Elevated=Color(0xFF172740); private val Cyan=Color(0xFF12B8F5); private val Violet=Color(0xFF8B5CF6); private val Muted=Color(0xFFA9B8CC)
private val Languages=listOf("English","العربية","Français","Español","Deutsch","Italiano","Português","Türkçe","Русский","中文","日本語","한국어","हिन्दी","فارسی","اردو","বাংলা","Bahasa Indonesia","ไทย","Nederlands","Polski")
data class AppRow(val label:String,val pkg:String,val version:String,val icon:android.graphics.drawable.Drawable?,val source:String?)
class MainVm:ViewModel(){
    var tab by mutableStateOf(0)
    var currentDir by mutableStateOf(Environment.getExternalStorageDirectory())
    var language by mutableStateOf("English")
    var settings by mutableStateOf(false)
    var toolMessage by mutableStateOf<String?>(null)
    var files by mutableStateOf(listOf<File>())
    var apps by mutableStateOf(listOf<AppRow>())
    var query by mutableStateOf("")
    var filesLoading by mutableStateOf(false)
    var appsLoading by mutableStateOf(false)
    private var filesJob: Job? = null
    private var appsJob: Job? = null

    fun loadFiles(dir: File = currentDir) {
        currentDir = dir
        filesJob?.cancel()
        filesLoading = true
        filesJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    dir.listFiles()?.asSequence()
                        ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
                        ?.take(250)?.toList() ?: emptyList()
                }.getOrDefault(emptyList())
            }
            if (currentDir.absolutePath == dir.absolutePath) files = result
            filesLoading = false
        }
    }

    fun loadApps(ctx: Context, force: Boolean = false) {
        if (apps.isNotEmpty() && !force) return
        appsJob?.cancel()
        appsLoading = true
        appsJob = viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                ctx.packageManager.getInstalledApplications(0).asSequence().mapNotNull { info ->
                    runCatching {
                        val p = ctx.packageManager.getPackageInfo(info.packageName, 0)
                        AppRow(info.loadLabel(ctx.packageManager).toString(), info.packageName, p.versionName ?: "—", info.loadIcon(ctx.packageManager), info.sourceDir)
                    }.getOrNull()
                }.sortedBy { it.label.lowercase() }.take(300).toList()
            }
            apps = result
            appsLoading = false
        }
    }
}
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = Cyan, secondary = Violet, background = Navy, surface = Surface, onSurface = Color.White),
        content = content
    )
}
class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) { super.onCreate(b); setContent { AppTheme { Root() } } }
}
@Composable
fun Root(vm: MainVm = viewModel()) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { vm.loadFiles(); vm.loadApps(ctx) }
    Scaffold(containerColor = Navy, bottomBar = { BottomBar(vm) }) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                vm.settings -> SettingsScreen(vm)
                vm.tab == 0 -> Home(vm)
                vm.tab == 1 -> Files(vm)
                vm.tab == 2 -> Apps(vm)
                vm.tab == 3 -> Storage()
                else -> Tools(vm)
            }
        }
    }
    vm.toolMessage?.let { msg ->
        AlertDialog(onDismissRequest = { vm.toolMessage = null }, confirmButton = { TextButton({ vm.toolMessage = null }) { Text("OK") } }, title = { Text("Wissam File Master") }, text = { Text(msg) })
    }
}
@Composable fun Header(title:String,icon:ImageVector=Icons.Default.Folder,onBack:(()->Unit)?=null,action:()->Unit={}){Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=16.dp),verticalAlignment=Alignment.CenterVertically){if(onBack!=null)IconButton(onBack){Icon(Icons.Default.ArrowBack,null)}else Icon(icon,null,tint=Cyan,modifier=Modifier.size(34.dp));Spacer(Modifier.width(10.dp));Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(action){Icon(Icons.Default.MoreVert,null)}}}
@Composable fun Home(vm:MainVm){val ctx=LocalContext.current;LazyColumn(Modifier.fillMaxSize().padding(horizontal=14.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=20.dp)){item{Row(Modifier.padding(top=14.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Folder,null,tint=Cyan,modifier=Modifier.size(38.dp));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text("Wissam",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);Text("File Master",color=Cyan,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);Text("All-in-One File Manager • APK Tools",color=Muted,style=MaterialTheme.typography.labelSmall)};IconButton({vm.settings=true}){Icon(Icons.Default.Settings,null)}}};item{StorageCard()};item{Text("Quick Access",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)};item{LazyVerticalGrid(GridCells.Fixed(3),Modifier.height(220.dp),userScrollEnabled=false,verticalArrangement=Arrangement.spacedBy(10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){items(listOf("Apps" to Icons.Default.Android,"Downloads" to Icons.Default.Download,"Images" to Icons.Default.Image,"Videos" to Icons.Default.PlayArrow,"Documents" to Icons.Default.Description,"Archives" to Icons.Default.FolderZip)){(t,i)->Quick(t,i){if(t=="Apps")vm.tab=2 else {vm.tab=1;vm.loadFiles(File(Environment.getExternalStorageDirectory(),when(t){"Downloads"->"Download";"Images"->"DCIM";"Videos"->"Movies";"Documents"->"Documents";else->"."}))}}}}};item{Text("Recent Files",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)};items(vm.files.take(5)){FileItem(it,{if(it.isDirectory)vm.loadFiles(it)})}}}
@Composable fun StorageCard(){val s=StatFs(Environment.getExternalStorageDirectory().path);val total=s.totalBytes.toDouble();val free=s.availableBytes.toDouble();val used=(total-free).coerceAtLeast(0.0);val pct=(used/total).toFloat().coerceIn(0f,1f);Card(colors=CardDefaults.cardColors(containerColor=Elevated),shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(18.dp)){Text("Internal Storage",color=Muted);Row(verticalAlignment=Alignment.Bottom){Text(gb(total),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Spacer(Modifier.width(6.dp));Text("Total",color=Muted);Spacer(Modifier.weight(1f));Text("${(pct*100).toInt()}%",color=Cyan,fontWeight=FontWeight.Bold)};LinearProgressIndicator(progress={pct},modifier=Modifier.fillMaxWidth().padding(vertical=10.dp).height(9.dp),color=Cyan,trackColor=Violet);Row{Text("${gb(used)} Used",color=Muted);Spacer(Modifier.weight(1f));Text("${gb(free)} Free",color=Muted)}}}}
private fun gb(x:Double)=String.format("%.1f GB",x/1e9)
@Composable fun Quick(t:String,i:ImageVector,onClick:()->Unit){Card(Modifier.clickable(onClick=onClick),colors=CardDefaults.cardColors(containerColor=Surface),shape=RoundedCornerShape(15.dp)){Column(Modifier.fillMaxSize().padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Icon(i,null,tint=if(t=="Apps")Color(0xFF7CFF65) else Cyan,modifier=Modifier.size(28.dp));Text(t,fontWeight=FontWeight.Bold);Text("Open",color=Muted,style=MaterialTheme.typography.labelSmall)}}}
@Composable fun Files(vm:MainVm){val ctx=LocalContext.current;Column{Header("Files",onBack=if(vm.currentDir.path!=Environment.getExternalStorageDirectory().path)({vm.loadFiles(vm.currentDir.parentFile?:Environment.getExternalStorageDirectory())})else null,action={vm.toolMessage="Use Quick Access or tap any folder to browse it. Long operations respect Android storage access rules."});Row(Modifier.padding(horizontal=16.dp).fillMaxWidth().background(Elevated,RoundedCornerShape(16.dp)).padding(12.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.FolderOpen,null,tint=Cyan);Spacer(Modifier.width(8.dp));Text(vm.currentDir.path,color=Muted,maxLines=1)};if(vm.filesLoading) LinearProgressIndicator(Modifier.fillMaxWidth(),color=Cyan);LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){items(vm.files){f->FileItem(f){if(f.isDirectory)vm.loadFiles(f)else openFile(ctx,f)}}}}}
@Composable fun FileItem(f:File,onClick:()->Unit){Row(Modifier.fillMaxWidth().clickable(onClick=onClick).background(Surface,RoundedCornerShape(14.dp)).padding(12.dp),verticalAlignment=Alignment.CenterVertically){Icon(if(f.isDirectory)Icons.Default.Folder else Icons.Default.InsertDriveFile,null,tint=if(f.isDirectory)Color(0xFFFFB52E) else Cyan,modifier=Modifier.size(34.dp));Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(f.name,fontWeight=FontWeight.Bold);Text(if(f.isDirectory)"Folder" else "${f.length()/1024} KB • ${DateFormat.getDateInstance().format(Date(f.lastModified()))}",color=Muted,style=MaterialTheme.typography.bodySmall)};Icon(Icons.Default.ChevronRight,null,tint=Muted)}}
private fun openFile(ctx:Context,f:File){try{val uri=FileProvider.getUriForFile(ctx,ctx.packageName+".provider",f);ctx.startActivity(Intent(Intent.ACTION_VIEW).apply{setDataAndType(uri,"*/*");addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)})}catch(_:Exception){ctx.startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="*/*";addCategory(Intent.CATEGORY_OPENABLE)})}}
@Composable fun Apps(vm:MainVm){val ctx=LocalContext.current;var menu by remember{mutableStateOf(false)};Column{Header("APK Manager",Icons.Default.Android,action={menu=true});if(menu){DropdownMenu(true,{menu=false}){DropdownMenuItem({Text("Sort by name")},{menu=false});DropdownMenuItem({Text("Refresh apps")},{vm.loadApps(ctx, force = true);menu=false});DropdownMenuItem({Text("Visible apps: ${vm.apps.size}")},{menu=false})}};Text(if(vm.appsLoading)"Loading applications…" else "${vm.apps.size} visible applications",Modifier.padding(horizontal=16.dp),color=Muted);if(vm.appsLoading) LinearProgressIndicator(Modifier.fillMaxWidth(),color=Cyan);LazyColumn(contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){items(vm.apps.filter{it.label.contains(vm.query,true)||it.pkg.contains(vm.query,true)}){a->AppItem(a)}}}}
@Composable fun AppItem(a:AppRow){val ctx=LocalContext.current;var menu by remember{mutableStateOf(false)};Row(Modifier.fillMaxWidth().background(Surface,RoundedCornerShape(14.dp)).padding(10.dp),verticalAlignment=Alignment.CenterVertically){if(a.icon!=null)androidx.compose.foundation.Image(a.icon.asPainter(),null,Modifier.size(42.dp))else Icon(Icons.Default.Android,null,tint=Cyan);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(a.label,fontWeight=FontWeight.Bold);Text("${a.version} • ${a.pkg}",color=Muted,style=MaterialTheme.typography.bodySmall)};Box{IconButton({menu=true}){Icon(Icons.Default.MoreVert,null)};DropdownMenu(menu,{menu=false}){DropdownMenuItem({Text("Launch")},{menu=false;try{ctx.startActivity(ctx.packageManager.getLaunchIntentForPackage(a.pkg))}catch(_:Exception){}});DropdownMenuItem({Text("App info")},{menu=false;ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${a.pkg}")))});DropdownMenuItem({Text("Extract / Share APK")},{menu=false;shareApk(ctx,a)});DropdownMenuItem({Text("Copy package name")},{menu=false})}}}}
private fun shareApk(ctx:Context,a:AppRow){try{val src=a.source?:return;val out=File(ctx.cacheDir,"${a.pkg}.apk");File(src).copyTo(out,true);val uri=FileProvider.getUriForFile(ctx,ctx.packageName+".provider",out);ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="application/vnd.android.package-archive";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Share extracted APK"))}catch(e:Exception){ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,a.pkg)},"Share package name"))}}
@Composable fun Storage(){Column{Header("Storage Analyzer",Icons.Default.PieChart);StorageCard();Text("Accessible storage scope",Modifier.padding(18.dp),color=Muted);Text("Live totals are from shared external storage. The app does not claim a private-app or full-device scan.",Modifier.padding(horizontal=18.dp),color=Color.White)}}
@Composable fun Tools(vm:MainVm){Column{Header("Tools",Icons.Default.Build);LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(listOf("Create / Extract ZIP","Duplicate Finder","Large File Finder","Text & PDF Preview","Settings")){t->Card(Modifier.fillMaxWidth().clickable{when(t){"Settings"->vm.settings=true;"Text & PDF Preview"->vm.toolMessage="Choose a file from the Files screen to open it with Android's viewer.";"Large File Finder"->vm.toolMessage="Large-file scan is scoped to the currently accessible shared storage.";"Duplicate Finder"->vm.toolMessage="Duplicate matching is planned with safe hash confirmation; no files are auto-deleted.";else->vm.toolMessage="Use Android's document picker to select an archive. ZIP operations will never delete the original."}},colors=CardDefaults.cardColors(containerColor=Surface)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Icon(if(t=="Settings")Icons.Default.Settings else Icons.Default.Build,null,tint=Violet);Spacer(Modifier.width(14.dp));Text(t,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Icon(Icons.Default.ChevronRight,null)}}}}}}
@Composable fun SettingsScreen(vm:MainVm){Column{Header("Settings",Icons.Default.Settings,onBack={vm.settings=false});LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("Appearance",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)};item{SettingRow("Dark theme","Enabled for the reference visual style")};item{Text("Language",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=12.dp))};items(Languages){lang->Row(Modifier.fillMaxWidth().clickable{vm.language=lang}.background(if(vm.language==lang)Elevated else Surface,RoundedCornerShape(12.dp)).padding(16.dp),verticalAlignment=Alignment.CenterVertically){Text(lang,Modifier.weight(1f));if(vm.language==lang)Icon(Icons.Default.Check,null,tint=Cyan)}};item{Text("Selected language: ${vm.language}",color=Cyan,modifier=Modifier.padding(top=8.dp))};item{Text("Privacy: offline-first, no analytics, no cloud upload.",color=Muted,modifier=Modifier.padding(top=18.dp))}}}}
@Composable fun SettingRow(a:String,b:String){Row(Modifier.fillMaxWidth().background(Surface,RoundedCornerShape(12.dp)).padding(16.dp)){Column{Text(a,fontWeight=FontWeight.Bold);Text(b,color=Muted,style=MaterialTheme.typography.bodySmall)}}}
@Composable fun BottomBar(vm:MainVm){NavigationBar(containerColor=Surface){listOf("Home" to Icons.Default.Home,"Files" to Icons.Default.Folder,"Apps" to Icons.Default.Android,"Storage" to Icons.Default.PieChart,"Tools" to Icons.Default.Build).forEachIndexed{idx,(t,i)->NavigationBarItem(selected=vm.tab==idx,onClick={vm.tab=idx},icon={Icon(i,null)},label={Text(t)})}}}
