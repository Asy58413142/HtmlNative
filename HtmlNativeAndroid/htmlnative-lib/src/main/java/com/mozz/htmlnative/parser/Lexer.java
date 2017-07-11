package com.mozz.htmlnative.parser;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mozz.htmlnative.HNLog;
import com.mozz.htmlnative.HNSegment;
import com.mozz.htmlnative.common.CharQueue;
import com.mozz.htmlnative.exception.HNSyntaxError;
import com.mozz.htmlnative.parser.syntaxexc.SyntaxErrorHandler;
import com.mozz.htmlnative.parser.syntaxexc.SyntaxExceptionSource;
import com.mozz.htmlnative.parser.token.Token;
import com.mozz.htmlnative.parser.token.TokenType;
import com.mozz.htmlnative.reader.TextReader;

import java.io.EOFException;

import static com.mozz.htmlnative.HNLog.LEXER;


class Lexer implements SyntaxExceptionSource {

    private TextReader mReader;

    @NonNull
    private StringBuilder mBuffer = new StringBuilder();

    private int mLookFor = 0;

    private static final int LK_NOTHING = 1;
    private static final int LK_INNER = 1 << 1;

    private CharQueue mCacheQueue;

    /**
     * Max cache size of chars, stores the character picked from source code. See
     * {@link CharQueue} for more information. This size is used by {@link Lexer#mCacheQueue}
     */
    private static final int CACHE_SIZE = 7;

    private int mReserved = 0;

    private char mCurrent = TextReader.INIT_CHAR;

    private boolean mIsInStyle = false;

    private SyntaxErrorHandler mSyntaxErrorHandler;

    /**
     * Add for recognize code from Inner Element. If < script > is meet, than mLookForScript==3.
     * <p>
     * otherwise, mLookForScript < 3.
     * <p>
     * When met <, mLookForScript = 1;
     * <p>
     * When met Script, mLookForScript++;
     * <p>
     * When met >, mLookForScript++;
     * <p>
     * Otherwise, mLookForScript = 0;
     */
    private int mLookForScript = 0;

    Lexer(TextReader reader, SyntaxErrorHandler syntaxErrorHandler) {
        mReader = reader;
        mCacheQueue = new CharQueue(CACHE_SIZE);
        mSyntaxErrorHandler = syntaxErrorHandler;
        mSyntaxErrorHandler.setSource(this);
        lookFor(LK_NOTHING);
    }

    @Nullable
    Token scan() throws EOFException, HNSyntaxError {
        while (true) {
            this.skipWhiteSpace();

            long line = getLine();
            long column = getColumn();

            switch (peek()) {
                case '<':
                    mLookForScript = 1;
                    lookFor(LK_NOTHING);
                    next();
                    return Token.obtainToken(TokenType.StartAngleBracket, line, column);

                case '"':
                    next();
                    mLookForScript = 0;
                    return scanValue();
                case '>':
                    mLookForScript++;
                    lookFor(LK_INNER);
                    next();
                    return Token.obtainToken(TokenType.EndAngleBracket, line, column);

                case '/':
                    mLookForScript = 0;
                    next();
                    return Token.obtainToken(TokenType.Slash, line, column);

                case '=':
                    mLookForScript = 0;
                    next();
                    return Token.obtainToken(TokenType.Equal, line, column);

                case '{':
                    mLookForScript = 0;
                    next();
                    return Token.obtainToken(TokenType.StartBrace, line, column);

                case '}':
                    mLookForScript = 0;
                    next();
                    return Token.obtainToken(TokenType.EndBrace, line, column);

                case '#':
                    mLookForScript = 0;
                    next();
                    return Token.obtainToken(TokenType.Hash, line, column);

                case '*':
                    mLookForScript = 0;
                    next();
                    return Token.obtainToken(TokenType.Star, line, column);

                case ',':
                    mLookForScript = 0;
                    next();
                    return Token.obtainToken(TokenType.Comma, line, column);

                case '.':
                    mLookForScript = 0;
                    next();
                    return Token.obtainToken(TokenType.Dot, line, column);

                case ':':
                    mLookForScript = 0;
                    next();
                    return Token.obtainToken(TokenType.Colon, line, column);

                case ';':
                    mLookForScript = 0;
                    next();
                    return Token.obtainToken(TokenType.Semicolon, line, column);

                case '(':
                    mLookForScript = 0;
                    next();
                    return Token.obtainToken(TokenType.StartParen, line, column);

                case ')':
                    mLookForScript = 0;
                    next();
                    return Token.obtainToken(TokenType.EndParen, line, column);

                case '!':
                    mLookForScript = 0;
                    next();
                    return Token.obtainToken(TokenType.Exclamation, line, column);

            }

            if (isLookingFor(LK_INNER) && mLookForScript < 3 && peek() != '<' && !mIsInStyle) {
                return scanInner();
            }

            if (isDigit(peek()) || peek() == '-') {
                mLookForScript = 0;
                return scanNumber();
            }

            if (isLetter(peek()) || peek() == '_') {
                return scanId();
            }

            mSyntaxErrorHandler.throwException("unknown token " + peek(), line, column);
        }
    }

    public void skipUntil(char c) throws EOFException {
        for (; ; ) {
            char ch = peek();
            if (ch != c) {
                next();
            } else {
                break;
            }
        }
    }

    @Nullable
    private Token scanNumber() throws EOFException, HNSyntaxError {
        long startColumn = getColumn();
        long line = getLine();
        int v = 0;
        boolean negative = false;
        if (peek() == '-') {
            negative = true;
            next();
        }

        while (!Lexer.isDigit(peek())) {
            mSyntaxErrorHandler.throwException("Illegal word when reading Number!" + peek(),
                    line, startColumn);
        }

        do {
            v = 10 * v + (peek() - '0');
            next();
        } while (isDigit(peek()));

        if (peek() != '.' && peek() != 'E' && peek() != 'e' && peek() != '%') {
            return Token.obtainToken(TokenType.Int, negative ? -v : v, line, startColumn);
        }

        if (peek() == '%') {
            next();
            return Token.obtainToken(TokenType.Double, negative ? -v / 100.f : v / 100.f, line,
                    startColumn, Token.EXTRA_NUMBER_PERCENTAGE);
        }

        double x = v, d = 10;
        if (peek() == '.') {
            for (; ; ) {
                next();
                if (!Lexer.isDigit(peek())) {
                    break;
                }

                x = x + (peek() - '0') / d;
                d = d * 10;
            }
        }

        if (peek() == '%') {
            next();
            return Token.obtainToken(TokenType.Double, negative ? -x / 100.f : x / 100.f, line,
                    startColumn, Token.EXTRA_NUMBER_PERCENTAGE);
        }

        mReserved = 1;
        if (peek() == 'e' || peek() == 'E') {

            //consume the e or E
            next();

            next();

            if (!Lexer.isDigit(peek()) && peek() != '-') {
                return Token.obtainToken(TokenType.Double, negative ? -x : x, line, startColumn);
            }
            boolean expIsNegative = false;
            if (peek() == '-') {
                expIsNegative = true;
                next();
            }

            int n = 0;
            do {
                n = 10 * n + (peek() - '0');
                next();
            } while (Lexer.isDigit(peek()));

            n = expIsNegative ? -n : n;

            double exp = Math.pow(10, n);
            return Token.obtainToken(TokenType.Double, negative ? (-x * exp) : (x * exp), line,
                    startColumn);

        } else {
            return Token.obtainToken(TokenType.Double, negative ? -x : x, line, startColumn);
        }
    }

    @Nullable
    private Token scanId() throws EOFException {
        long startColumn = getColumn();
        long line = getLine();

        clearBuf();

        do {
            mBuffer.append(peek());
            next();
        }
        while (isLetter(peek()) || isDigit(peek()) || peek() == '.' || peek() == '-' || peek() ==
                '_');

        String idStr = mBuffer.toString();

        TokenType type = TokenType.Id;
        String tokenContent;

        if (idStr.equalsIgnoreCase(TokenType.Template.toString()) || idStr.equalsIgnoreCase
                (TokenType.Body.toString())) {

            type = TokenType.Template;
            tokenContent = idStr;

        } else if (idStr.equalsIgnoreCase(TokenType.Script.toString())) {
            mLookForScript++;

            type = TokenType.Script;
            tokenContent = idStr;

        } else if (idStr.equalsIgnoreCase(TokenType.Head.toString())) {

            type = TokenType.Head;
            tokenContent = idStr;

        } else if (idStr.equalsIgnoreCase(TokenType.Meta.toString())) {

            type = TokenType.Meta;
            tokenContent = idStr;

        } else if (idStr.equalsIgnoreCase(TokenType.Link.toString())) {

            type = TokenType.Link;
            tokenContent = idStr;
        } else if (idStr.equalsIgnoreCase(TokenType.Html.toString())) {

            type = TokenType.Html;
            tokenContent = idStr;

        } else if (idStr.equalsIgnoreCase(TokenType.Title.toString())) {

            type = TokenType.Title;
            tokenContent = idStr;
        } else if (idStr.equalsIgnoreCase(TokenType.Style.toString())) {
            type = TokenType.Style;
            tokenContent = idStr;

            mIsInStyle = !mIsInStyle && peekHistory(6) == '<';

        } else {
            tokenContent = idStr;
        }

        return Token.obtainToken(type, tokenContent, line, startColumn);

    }

    @Nullable
    private Token scanValue() throws EOFException {
        long startColumn = getColumn();
        long line = getLine();

        clearBuf();

        if (peek() == '"') {
            next();
            return Token.obtainToken(TokenType.Value, "", line, startColumn);
        }

        do {
            mBuffer.append(peek());
            next();

            // handling the '\"' case
            if (peek() == '\\') {
                next();
                if (peek() != '"') {
                    mBuffer.append('\\');
                }
            } else if (peek() == '"') {
                break;
            }
        } while (true);

        next();

        return Token.obtainToken(TokenType.Value, mBuffer.toString(), line, startColumn);

    }

    @Nullable
    private Token scanInner() throws EOFException {
        long startColumn = getColumn();
        long line = getLine();

        clearBuf();

        do {
            mBuffer.append(peek());
            next();

            if (peek() == '\\') {
                next();
                if (peek() != '<') {
                    mBuffer.append('\\');
                }
            } else if (peek() == '<') {
                break;
            }

            //TODO 考虑其他的情况，这里只会添加一个空格
            if (skipWhiteSpaceInner()) {
                mBuffer.append(' ');
            }

        } while (peek() != '<');

        lookFor(LK_NOTHING);

        char lastChar = mBuffer.charAt(mBuffer.length() - 1);
        if (lastChar == '\n' || lastChar == '\r') {
            mBuffer.deleteCharAt(mBuffer.length() - 1);
        }
        return Token.obtainToken(TokenType.Inner, mBuffer.toString(), line, startColumn);
    }

    /**
     * Called by {@link Parser#processScript(HNSegment)}, not by Lexer, the structure may ugly
     * but simple to implement.  Because Lexer
     * can't tell whether it's an script or not, only parser has such ability.
     * <br/>
     * This function read the script inside "script" tag, no matter it's JavaScript or Lua or
     * Other language. It detect the end of script by reading '<' and '/' continuously outside the
     * quotation;
     *
     * @return ScriptInfo string
     * @throws EOFException
     * @throws HNSyntaxError
     */
    final Token scanScript() throws EOFException, HNSyntaxError {
        long startColumn = getColumn();
        long line = getLine();

        while (currentPositionInFile() < CACHE_SIZE) {
            mSyntaxErrorHandler.throwException("wrong status, too early for script.", line,
                    startColumn);
        }


        clearBuf();

        /*
         * to handle the case <script></script>, in this case, before 2 next(), peek() is already
          * point to '<'
         */
        int meetEndTagFirst = 0;

        if (peek() == '<') {
            meetEndTagFirst++;
        }

        next();

        if (peek() == '/') {
            meetEndTagFirst++;
        }

        if (meetEndTagFirst == 2) {
            mReserved = 2;
            next();
            return Token.obtainToken(TokenType.ScriptCode, "", line, startColumn);
        }

        next();

        // 0 no in any quota, 1 for quotation, 2 for single quotation
        byte inQuotation = 0;
        while (true) {
            if (inQuotation == 0 && peekHistory(0) == '/' && peekHistory(1) == '<') {
                mReserved = 2;
                next();
                break;
            }
            char ch = peekHistory(1);

            if (inQuotation == 0) {
                if (ch == '"') {
                    inQuotation = 1;
                } else if (ch == '\'') {
                    inQuotation = 2;
                }
            } else {
                if (inQuotation == 1) {
                    if (ch == '"' && peekHistory(4) != '\\') {
                        inQuotation = 0;
                    }
                } else if (inQuotation == 2) {
                    if (ch == '\'' && peekHistory(4) != '\\') {
                        inQuotation = 0;
                    }
                }
            }

            mBuffer.append(ch);
            next();
        }

        return Token.obtainToken(TokenType.ScriptCode, mBuffer.toString(), line, startColumn);
    }


    private boolean skipWhiteSpaceInner() throws EOFException {
        boolean meet = false;
        for (; ; ) {
            char ch = peek();
            if (ch == ' ' || ch == '\r' || ch == '\n' || ch == '\t' || ch == '\f' || ch == '\b') {
                if (!meet) {
                    meet = true;
                }
                next();
            } else {
                break;
            }
        }

        return meet;
    }


    public void skipWhiteSpace() throws EOFException {
        for (; ; ) {
            char ch = peek();
            if (ch == ' ' || ch == '\r' || ch == '\n' || ch == '\t' || ch == '\f' || ch == '\b') {
                next();
            } else {
                break;
            }
        }
    }

    public void close() {
        if (mReader != null) {
            mReader.close();
        }
    }

    @Override
    public long getLine() {
        return mReader.line();
    }

    @Override
    public long getColumn() {
        return mReader.column();
    }

    @Override
    public void onSyntaxException() throws HNSyntaxError, EOFException {
        next();
    }

    public char peek() {
        return mCurrent;
    }

    private long currentPositionInFile() {
        return mReader.countOfRead();
    }


    /**
     * @param historyBackCount must be smaller than {@link Lexer#CACHE_SIZE}
     * @return history char saved in {@link Lexer#mCacheQueue}
     */
    private char peekHistory(int historyBackCount) {
        if (historyBackCount > CACHE_SIZE) {
            throw new IllegalArgumentException("HistoryBackCount must be smaller than CACHE_SIZE " +
                    "" + "" + "" + "" + "" + "" + "" + "" + "" + "(" + CACHE_SIZE + ")");
        }

        return mCacheQueue.peekHistory(historyBackCount);
    }

    public void next() throws EOFException {
        if (mReserved > 0) {
            mCurrent = mCacheQueue.peek(CACHE_SIZE - mReserved);
            mReserved--;
            return;
        }
        this.mReader.nextCh();
        mCurrent = this.mReader.current();
        mCacheQueue.push(peek());
        HNLog.d(LEXER, "next-> " + peek());
    }

    private void lookFor(int status) {
        mLookFor = 0;
        mLookFor |= status;
    }

    private boolean isLookingFor(int status) {
        return (mLookFor & status) != 0;
    }

    public static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    public static boolean isLetter(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
    }

    private void clearBuf() {
        mBuffer.setLength(0);
    }
}
