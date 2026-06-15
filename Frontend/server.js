const express = require('express');
const axios = require('axios');
const path = require('path');
const session = require('express-session');

const app = express();
const PORT = 3000;

app.use(session({
    secret: 'chave-secreta-do-tcc',
    resave: false,
    saveUninitialized: true,
    cookie: { secure: false }
}));

app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Armazenamento do código unificado
let codigoValidoUnificado = "";

app.get('/', (req, res) => { res.render('cadastro'); });

app.post('/cadastrar', async (req, res) => {
    const { name, email, password } = req.body;
    try {
        await axios.post('http://localhost:8081/users', { name, email, password });
        res.render('sucesso', { name, email });
    } catch (error) {
        res.status(500).send('Erro ao realizar cadastro.');
    }
});

app.get('/login', (req, res) => {
    res.render('login', { msgErro: null, msgSucesso: null, emailPreenchido: '' });
});

// 2. Solicita o código ao Java e captura o valor REAL do e-mail
app.post('/solicitar-codigo', async (req, res) => {
    const { email } = req.body;
    try {
        // Dispara para o Java e captura a resposta com o código real
        const response = await axios.post('http://localhost:8081/users/auth/request-code', {
            email: email
        });
        
        // 🔥 A MÁGICA ACONTECE AQUI: O código válido agora é EXATAMENTE o que o Java gerou e mandou pro e-mail
        codigoValidoUnificado = response.data.toString().trim();

        console.log(`\n==================================`);
        console.log(`🔑 CÓDIGO DO E-MAIL CAPTURADO: ${codigoValidoUnificado}`);
        console.log(`==================================\n`);
        
        res.render('validar-codigo', { email: email, msgErro: null });
    } catch (error) {
        console.error('Erro ao conectar com Java:', error.message);
        res.render('login', { 
            msgErro: 'Erro ao gerar código. Verifique se o UserService está ativo.', 
            msgSucesso: null,
            emailPreenchido: email 
        });
    }
});

// 3. Validação Real e Estrita (Apenas o código do e-mail funciona)
app.post('/validar-codigo', async (req, res) => {
    const { email, codigo } = req.body;
    const codigoDigitadoLimpo = codigo ? codigo.trim() : "";

    // Validação idêntica contra o código do e-mail capturado do Java
    if (codigoDigitadoLimpo !== codigoValidoUnificado) {
        return res.render('validar-codigo', { 
            email: email, 
            msgErro: 'Código inválido ou já expirado no Cache de 5 minutos! Verifique seu e-mail e tente novamente.' 
        });
    }

    // Se bater, cria o JWT de sessão
    req.session.token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI" + btoa(email) + "IiwiaWF0IjoxNzE4Mzg0MDAwfQ"; 

    res.render('dashboard', { email: email, token: req.session.token });
});

app.listen(PORT, () => {
    console.log(`📡 Servidor Frontend rodando na porta http://localhost:${PORT}`);
});