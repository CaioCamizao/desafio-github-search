package br.com.igorbag.githubsearch.ui

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.igorbag.githubsearch.R
import br.com.igorbag.githubsearch.data.GitHubService
import br.com.igorbag.githubsearch.domain.Repository
import br.com.igorbag.githubsearch.ui.adapter.RepositoryAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var nomeUsuario: EditText
    private lateinit var btnConfirmar: Button
    private lateinit var listaRepositories: RecyclerView
    private lateinit var githubApi: GitHubService
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val PREF_NAME = "MinhasPreferencias"
        private const val KEY_USER_NAME = "nomeUsuario"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupView()
        setupListeners()
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        showUserName()
        setupRetrofit()
    }

    private fun setupView() {
        nomeUsuario = findViewById(R.id.et_nome_usuario)
        btnConfirmar = findViewById(R.id.btn_confirmar)
        listaRepositories = findViewById(R.id.rv_lista_repositories)
    }

    private fun setupListeners() {
        btnConfirmar.setOnClickListener {
            val nomeUsuarioTexto = nomeUsuario.text.toString()
            saveUserLocal(nomeUsuarioTexto)
            getAllReposByUserName(nomeUsuarioTexto)
        }
    }

    private fun saveUserLocal(nomeUsuarioTexto: String) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY_USER_NAME, nomeUsuarioTexto)
        editor.apply()
        Toast.makeText(this@MainActivity, "Usuário salvo localmente.", Toast.LENGTH_SHORT).show()
    }

    private fun showUserName() {
        if (sharedPreferences.contains(KEY_USER_NAME)) {
            val nomeUsuarioSalvo = sharedPreferences.getString(KEY_USER_NAME, "")
            nomeUsuario.setText(nomeUsuarioSalvo)
        }
    }

    private fun setupRetrofit() {
        val baseUrl = "https://api.github.com/"
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        githubApi = retrofit.create(GitHubService::class.java)
    }

    private fun getAllReposByUserName(nomeDoUsuario: String) {
        val call = githubApi.getAllRepositoriesByUser(nomeDoUsuario)
        call.enqueue(object : Callback<List<Repository>> {
            override fun onResponse(call: Call<List<Repository>>, response: Response<List<Repository>>) {
                if (response.isSuccessful) {
                    val repositorios = response.body()
                    if (repositorios != null) {
                        setupAdapter(repositorios)
                    }
                } else {
                    showError()
                }
            }

            override fun onFailure(call: Call<List<Repository>>, t: Throwable) {
                showError()
            }
        })
    }

    private fun setupAdapter(list: List<Repository>) {
        val adapter = RepositoryAdapter(list, this::shareRepositoryLink, this::openBrowser)
        listaRepositories.adapter = adapter
        listaRepositories.layoutManager = LinearLayoutManager(this)
    }

    private fun showError() {
        Toast.makeText(this@MainActivity, "Erro ao buscar repositórios.", Toast.LENGTH_SHORT).show()
    }

    private fun shareRepositoryLink(urlRepository: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, urlRepository)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun openBrowser(urlRepository: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlRepository)))
    }
}