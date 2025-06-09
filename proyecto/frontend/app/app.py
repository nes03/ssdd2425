from flask import Flask, render_template, send_from_directory, url_for, request, redirect, flash
from flask_login import LoginManager, login_manager, current_user, login_user, login_required, logout_user
from forms import LoginForm, SignUpForm
from flask import jsonify,request, render_template
import requests
import os

# Usuarios
from models import users, User

# Login
from forms import LoginForm

app = Flask(__name__, static_url_path='') # Inicializar Flask
login_manager = LoginManager() # Configurar Flask-Login para gestionar sesiones de usuario
login_manager.init_app(app) # Para mantener la sesión

# Configurar el secret_key. OJO, no debe ir en un servidor git público.
# Python ofrece varias formas de almacenar esto de forma segura, que
# no cubriremos aquí.
app.config['SECRET_KEY'] = 'qH1vprMjavek52cv7Lmfe1FoCexrrV8egFnB21jHhkuOHm8hJUe1hwn7pKEZQ1fioUzDb3sWcNK1pJVVIhyrgvFiIrceXpKJBFIn_i9-LTLBCc4cqaI3gjJJHU6kxuT8bnC7Ng'

BACKEND_URL = f'http://localhost:8080/prompt'

@app.route('/static/<path:path>')
def serve_static(path):
    return send_from_directory('static', path)

@app.route('/')
def index():
    return render_template('index.html')


#Ruta para gestionar Registro de nuevos usuarios
@app.route('/signup', methods=['GET', 'POST'])
def signup():
    form = SignUpForm(request.form if request.method == 'POST' else None)
    if request.method == "POST" and form.validate():
        response = requests.post(
            'http://backend-rest:8080/Service/Registro',  
            json={
                "name": form.name.data,
                "email": form.email.data,
                "password": form.password.data
            }
        )
        if response.status_code == 200:
            flash("¡Registro completado! Puedes iniciar sesión.", "success")
            return redirect(url_for('signup'))
        else:
            flash(f"Registro no completado. Código: {response.status_code}. Mensaje: {response.text}", "danger")
    return render_template('signup.html', form=form)

#Ruta para gestionar Login de usuarios registrados
@app.route('/login', methods=['GET', 'POST'])
def login():
    if current_user.is_authenticated:
        return redirect(url_for('index'))
    error = None
    form = LoginForm(None if request.method != 'POST' else request.form)
    if request.method == "POST" and form.validate():
        response = requests.post(
            'http://backend-rest:8080/Service/checkLogin',  
            json={
                "email": form.email.data,
                "password": form.password.data
            }
        )
        if response.status_code == 200:
            user_data = response.json()
            user = User(user_data["id"], user_data["name"], form.email.data, form.password.data)
            login_user(user, remember=form.remember_me.data)
            return redirect(url_for('index'))
        else:
            error = 'Credenciales no válidas. Por favor, pruebe de nuevo.'
    return render_template('login.html', form=form, error=error)

#def login():
#    if current_user.is_authenticated:
#        return redirect(url_for('index'))
#    else:
#       error = None
#        form = LoginForm(None if request.method != 'POST' else request.form)
#        if request.method == "POST" and form.validate():
#            if form.email.data != 'admin@um.es' or form.password.data != 'admin':
#                error = 'Invalid Credentials. Please try again.'
#            else:
#                user = User(1, 'admin', form.email.data.encode('utf-8'),
#                            form.password.data.encode('utf-8'))
#                users.append(user)
#                login_user(user, remember=form.remember_me.data)
#                return redirect(url_for('index'))

#        return render_template('login.html', form=form,  error=error)

@app.route('/profile')
@login_required
def profile():
    response = requests.get(f'http://localhost:5010/api/user/{current_user.id}')
    if response.status_code == 200:
        response.json()
    else:
        flash('Error al obtener los datos del perfil.', 'danger')
    return render_template('profile.html')

# Ruta para cerrar sesión y redigir a index
@app.route('/logout')
@login_required
def logout():
    logout_user()
    return redirect(url_for('index'))



# Ruta para mostrar los logs de conversaciones
@app.route('/logs', methods=['GET', 'POST'])
def logs():
    if not current_user:
        flash("Acceso denegado", "danger")
        return redirect(url_for('index'))
    response = requests.get(f'http://localhost:8080/api/logs')
    logs = response.json() if response.status_code == 200 else []
    if request.method == 'POST':
        log_id = request.form.get('log_id')
        requests.delete(f'http://localhost:8080/api/logs/{log_id}')
        flash("Log eliminado correctamente", "success")
        return redirect(url_for('logs'))
    return render_template('logs.html', logs=logs)

# Ruta para ver estadisticas
@app.route('/stats')
def stats():
    response = requests.get(f'http://localhost:8080/api/stats')
    stats_data = response.json() if response.status_code == 200 else {}
    return render_template('stats.html', stats=stats_data)

@login_manager.user_loader
def load_user(user_id):
    for user in users:
        if user.id == int(user_id):
            return user
    return None
@app.route("/prompt", methods=["GET", "POST"])
def prompt():
    if request.method == "POST":
        prompt = request.form.get("prompt")

        # Enviar el prompt al API REST en Java
        try:
            response = requests.post(BACKEND_URL, json={"prompt": prompt})
            if response.status_code == 200:
                try:
                    data = response.json()
                    if "response" in data:
                        return render_template("prompt.html", prompt=prompt, response=data["response"])
                    else:
                        return render_template("prompt.html", error="Respuesta inesperada del servidor", prompt=prompt)
                except ValueError:
                    return render_template("prompt.html", error="Error al procesar la respuesta del servidor", prompt=prompt)
            else:
                return render_template("prompt.html", error=f"Error en el servidor Java: {response.text}", prompt=prompt)
        except Exception as e:
            return render_template("prompt.html", error=str(e), prompt=prompt)

    return render_template("prompt.html")

@app.route("/Service/prompt", methods=["POST"])
def proxy_prompt_to_backend():
    try:
        response = requests.post("http://backend-rest:8080/Service/prompt", json=request.get_json())
        return (response.text, response.status_code, response.headers.items())
    except Exception as e:
        return {"error": str(e)}, 500
    

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=int(os.environ.get('PORT', 5010)))