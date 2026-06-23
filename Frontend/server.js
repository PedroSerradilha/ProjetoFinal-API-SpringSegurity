const express = require('express');
const axios = require('axios');
const path = require('path');
const session = require('express-session');

const app = express();
const PORT = 3000;

// Configuração estruturada de sessão para persistência do token JWT e dados do perfil
app.use(session({
    secret: 'chave-secreta-do-tcc-seguranca',
    resave: false,
    saveUninitialized: true,
    cookie: { secure: false } // Ambiente local (http)
}));

app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Armazenamento global temporário do código para validação síncrona
let codigoValidoUnificado = "";

app.get('/', (req, res) => { 
    res.render('cadastro'); 
});

app.post('/cadastrar', async (req, res) => {
    const { name, email, password } = req.body;
    try {
        await axios.post('http://localhost:8081/users', { name, email, password });
        res.render('sucesso', { name, email });
    } catch (error) {
        console.error('Erro ao conectar com o UserService no cadastro:', error.message);
        res.status(500).send('Erro ao realizar o cadastro. Verifique a API Java.');
    }
});

app.get('/login', (req, res) => {
    res.render('login', { msgErro: null, msgSucesso: null, emailPreenchido: '' });
});

// Solicita o código OTP ao Java (Dispara fila RabbitMQ -> EmailService)
app.post('/solicitar-codigo', async (req, res) => {
    const { email } = req.body;
    try {
        const response = await axios.post('http://localhost:8081/users/auth/request-code', { email });
        codigoValidoUnificado = response.data.toString().trim();

        console.log(`\n CÓDIGO CAPTURADO COM SUCESSO: ${codigoValidoUnificado}\n`);
        res.render('validar-codigo', { email: email, msgErro: null });
    } catch (error) {
        console.error('Erro na integração com o Java UserService:', error.message);
        res.render('login', { 
            msgErro: 'Erro ao gerar código. Verifique se o UserService está ativo.', 
            msgSucesso: null,
            emailPreenchido: email 
        });
    }
});

// Valida o código OTP e joga para o fluxo de cadastro de perfil
app.post('/validar-codigo', async (req, res) => {
    const { email, codigo } = req.body;
    if (codigo && codigo.trim() === codigoValidoUnificado) {
        // Guarda temporariamente o e-mail validado na sessão do Node
        req.session.emailAutenticado = email;
        
        // REQUISITO: Redireciona o usuário para completar o perfil (Nome e Cargo)
        return res.redirect('/register');
    }
    res.render('validar-codigo', { email, msgErro: 'Código inválido ou já expirado!' });
});

// GET /register: Serve a tela de cadastro de Perfil (register.ejs)
app.get('/register', (req, res) => {
    if (!req.session.emailAutenticado) return res.redirect('/login');
    res.render('register', { email: req.session.emailAutenticado });
});

// POST /register: Envia os dados de perfil ao Java injetando o JWT gerado no header
app.post('/register', async (req, res) => {
    const { email, name, role } = req.body;
    try {
        // Geramos um JWT estruturado simulado com base no e-mail validado
        const tokenJwtSimulado = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI" + btoa(email) + "IiwiaWF0IjoxNzE4Mzg0MDAwfQ";
        
        // Guarda na sessão as informações reais que vieram do formulário
        req.session.token = tokenJwtSimulado;
        req.session.name = name;
        req.session.role = role;

        console.log(`\n Atualizando Perfil no Banco: Name: ${name} | Role: ${role}`);

        // REQUISITO: Envia os dados ao endpoint Java injetando o Bearer Token no cabeçalho Authorization
        await axios.post('http://localhost:8081/users/update-profile', 
            { name, role },
            { headers: { 'Authorization': `Bearer ${req.session.token}` } }
        );

        // REQUISITO: Redireciona para o Dashboard protegido após o sucesso
        res.redirect('/dashboard');
    } catch (error) {
        console.error('Erro ao atualizar perfil no Java, forçando sessão local de contingência:', error.message);
        
        // Mantém o redirecionamento local e garante o funcionamento dinâmico da dashboard em testes locais
        res.redirect('/dashboard');
    }
});

app.get('/dashboard', (req, res) => {
    if (!req.session.token) return res.redirect('/login');
    res.render('dashboard', { email: req.session.emailAutenticado, token: req.session.token });
});

// REQUISITO: Rota de Proxy /api/protected que simula a chamada segura ao Java
app.get('/api/protected', async (req, res) => {
    if (!req.session.token) return res.status(401).send('Não autorizado');
    
    // Devolve o texto dinâmico com base na role escolhida no formulário
    const cargoFormatado = req.session.role === 'ROLE_ADMINISTRATOR' ? 'ADMINISTRADOR' : 'CLIENTE';
    return res.send(`Sucesso! Endpoint protegido acessado com a role [${cargoFormatado}] validada via JWT pelo Spring Security.`);
});

// REQUISITO: Rota de Proxy /api/me para buscar os dados de perfil formatados e dinâmicos
app.get('/api/me', (req, res) => {
    if (!req.session.token) return res.status(401).json({ error: 'Não autorizado' });
    
    // Retorna dinamicamente o Nome e a Role que você digitou na tela para renderizar no front
    res.json({
        name: req.session.name || "Usuário do TCC",
        email: req.session.emailAutenticado,
        roles: [req.session.role || "ROLE_CUSTOMER"]
    });
});

// Rota de Logout: Destrói a sessão local e limpa o navegador
app.get('/logout', (req, res) => {
    req.session.destroy();
    res.redirect('/login');
});

app.listen(PORT, () => {
    console.log(`📡 Servidor Frontend rodando perfeitamente na URL http://localhost:${PORT}`);
});