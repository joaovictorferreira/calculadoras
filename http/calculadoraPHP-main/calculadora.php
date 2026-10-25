<?php
// Calculadora remota via POST

// Interpreta expressoes como (10+15)*4, usada na operacao 5
class Expressao {
    private $s;
    private $i = 0;

    private function __construct($texto) {
        $this->s = str_replace(' ', '', $texto);
    }

    public static function avaliar($texto) {
        $e = new Expressao($texto);
        $v = $e->soma();
        if ($e->i < strlen($e->s)) throw new Exception("expressao invalida");
        return $v;
    }

    //trata + e -
    private function soma() {
        $v = $this->produto();
        while ($this->i < strlen($this->s) && ($this->s[$this->i] == '+' || $this->s[$this->i] == '-')) {
            $op = $this->s[$this->i++];
            $b = $this->produto();
            $v = ($op == '+') ? $v + $b : $v - $b;
        }
        return $v;
    }

    //trata * e / . Fica num nivel mais interno que a soma, por isso resolve primeiro
    private function produto() {
        $v = $this->valor();
        while ($this->i < strlen($this->s) && ($this->s[$this->i] == '*' || $this->s[$this->i] == '/')) {
            $op = $this->s[$this->i++];
            $b = $this->valor();
            if ($op == '*') {
                $v = $v * $b;
            } else {
                if ($b == 0) throw new Exception("divisao por zero");
                $v = $v / $b;
            }
        }
        return $v;
    }

    //trata um numero ou um grupo entre parenteses
    private function valor() {
        if ($this->i >= strlen($this->s)) throw new Exception("expressao incompleta");

        if ($this->s[$this->i] == '(') {
            $this->i++;
            $v = $this->soma();
            if ($this->i >= strlen($this->s) || $this->s[$this->i] != ')') throw new Exception("falta fechar parentese");
            $this->i++;
            return $v;
        }

        $ini = $this->i;
        while ($this->i < strlen($this->s) && (ctype_digit($this->s[$this->i]) || $this->s[$this->i] == '.')) $this->i++;
        if ($ini == $this->i) throw new Exception("caractere inesperado");

        return floatval(substr($this->s, $ini, $this->i - $ini));
    }
}

// Função para validar e converter valores
function getPostValue($key) {
    return isset($_POST[$key]) ? floatval($_POST[$key]) : null;
}

$oper1 = getPostValue('oper1');
$oper2 = getPostValue('oper2');
$operacao = isset($_POST['operacao']) ? intval($_POST['operacao']) : null;
$expr = isset($_POST['expr']) ? $_POST['expr'] : null;

$resultado = null;
$erro = null;

if ($operacao === 5) {
    // Operacao 5: a expressao inteira e calculada aqui no servidor
    if ($expr === null || trim($expr) === '') {
        $erro = "Envie 'expr' via POST para a operacao 5.";
    } else {
        try {
            $resultado = Expressao::avaliar($expr);
        } catch (Exception $e) {
            $erro = "Erro: " . $e->getMessage();
        }
    }
} else if ($oper1 === null || $oper2 === null || $operacao === null) {
    $erro = "Parametros invalidos. Envie 'oper1', 'oper2' e 'operacao' via POST.";
} else {
    switch ($operacao) {
        case 1: // Soma
            $resultado = $oper1 + $oper2;
            break;
        case 2: // Subtracao
            $resultado = $oper1 - $oper2;
            break;
        case 3: // Multiplicacao
            $resultado = $oper1 * $oper2;
            break;
        case 4: // Divisao
            if ($oper2 == 0) {
                $erro = "Erro: divisao por zero.";
            } else {
                $resultado = $oper1 / $oper2;
            }
            break;
        default:
            $erro = "Operacao invalida. Use 1 (soma) 2 (subtracao) 3 (multiplicacao) 4 (divisao) ou 5 (expressao).";
    }
}

// Retorno em JSON
header('Content-Type: application/json');

if ($erro) {
    echo json_encode(["erro" => $erro]);
} else {
    echo json_encode([
        "oper1" => $oper1,
        "oper2" => $oper2,
        "operacao" => $operacao,
        "resultado" => $resultado
    ]);
}