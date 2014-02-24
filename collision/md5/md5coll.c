/*
 * MD5 collisions.
 *
 * This code implements the MD5 collision attack with the optimizations
 * introduced by Vlastimil Klima under the name "tunneling". See:
 *
 * Vlastimil Klima: Tunnels in Hash Functions: MD5 Collisions Within a
 * Minute, sent to IACR eprint, 18 March, 2006,
 * http://cryptography.hyperlink.cz/2006/tunnels.pdf
 *
 * Klima maintains an homepage on that subject, with some sample
 * demonstration code:
 * http://cryptography.hyperlink.cz/MD5_collisions.html
 *
 * This code has been inspired by Klima's sample code, although it has
 * been completely rewritten with some heavy structural changes, in
 * order to achieve better performance. This code is provided for
 * educational purposes only, without any explicit or implied guarantee
 * of merchantability or fitness for any purpose. This specific C
 * program was written by Thomas Pornin <thomas.pornin@cryptolog.com>,
 * on behalf of "Projet RNRT SAPHIR".
 */

/*
 * Note on notations:
 * ==================
 *
 * We use the notations introduced by Wang, Feng, Lai and Yu. Steps in
 * MD5 are numbered from 1 to 64. Intermediate values are called Q[1] to
 * Q[64]. Compression function input is Q[-3], Q[-2], Q[-1] and Q[0].
 *
 * Message words are numbered from 0 (X0, X1... X15) but within the
 * course of the algorithm, we refer to the "extended" words, after
 * duplication into the 64 words. The extended words are refered to as
 * X[1] to X[64]. Thus, X[1] = X0 (which is, admittedly, confusing).
 * The macros RMnn defined below perform that mapping.
 *
 * Occasionally, we refer to bits in a word by index. We number bits
 * from 0 (for least significant bit) to 31 (most significant). Klima's
 * paper numbers them from 1.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stddef.h>
#include <time.h>

/*
 * We use some types and macros defined by sphlib. We need sph_u32
 * (the "fast, 32-or-more bits unsigned type"), SPH_T32 ("explicit
 * truncate to 32 bits") and SPH_C32 (for defining literal constants
 * of the sph_u32 type).
 */

#include "sph_types.h"
#include "sph_md5.h"

#define T32    SPH_T32
#define C32    SPH_C32

/*
 * Left rotation of a 32-bit word. Decent C compilers recognize this
 * pattern and know how to use the rotation opcodes of the target
 * architecture, if some are provided.
 */
static inline sph_u32
RL32(sph_u32 x, int n)
{
	return T32((x << n) | (x >> (32 - n)));
}

/*
 * Right rotation of a 32-bit word.
 */
static inline sph_u32
RR32(sph_u32 x, int n)
{
	return T32((x << (32 - n)) | (x >> n));
}

/*
 * The four bitwise boolean functions for MD5. These expressions appear
 * to be the fastest in plain MD5 (as used in sphlib) so we reuse them
 * here.
 */
#define F(B, C, D)     ((((C) ^ (D)) & (B)) ^ (D))
#define G(B, C, D)     ((((C) ^ (B)) & (D)) ^ (C))
#define H(B, C, D)     ((B) ^ (C) ^ (D))
#define I(B, C, D)     ((C) ^ ((B) | T32(~(D))))

/*
 * RFnn is the boolean function for step nn.
 */

#define RF1     F
#define RF2     F
#define RF3     F
#define RF4     F
#define RF5     F
#define RF6     F
#define RF7     F
#define RF8     F
#define RF9     F
#define RF10    F
#define RF11    F
#define RF12    F
#define RF13    F
#define RF14    F
#define RF15    F
#define RF16    F

#define RF17    G
#define RF18    G
#define RF19    G
#define RF20    G
#define RF21    G
#define RF22    G
#define RF23    G
#define RF24    G
#define RF25    G
#define RF26    G
#define RF27    G
#define RF28    G
#define RF29    G
#define RF30    G
#define RF31    G
#define RF32    G

#define RF33    H
#define RF34    H
#define RF35    H
#define RF36    H
#define RF37    H
#define RF38    H
#define RF39    H
#define RF40    H
#define RF41    H
#define RF42    H
#define RF43    H
#define RF44    H
#define RF45    H
#define RF46    H
#define RF47    H
#define RF48    H

#define RF49    I
#define RF50    I
#define RF51    I
#define RF52    I
#define RF53    I
#define RF54    I
#define RF55    I
#define RF56    I
#define RF57    I
#define RF58    I
#define RF59    I
#define RF60    I
#define RF61    I
#define RF62    I
#define RF63    I
#define RF64    I

/*
 * RCnn is the "rotation count" for step nn.
 */

#define RC1     7
#define RC2    12
#define RC3    17
#define RC4    22
#define RC5     7
#define RC6    12
#define RC7    17
#define RC8    22
#define RC9     7
#define RC10   12
#define RC11   17
#define RC12   22
#define RC13    7
#define RC14   12
#define RC15   17
#define RC16   22

#define RC17    5
#define RC18    9
#define RC19   14
#define RC20   20
#define RC21    5
#define RC22    9
#define RC23   14
#define RC24   20
#define RC25    5
#define RC26    9
#define RC27   14
#define RC28   20
#define RC29    5
#define RC30    9
#define RC31   14
#define RC32   20

#define RC33    4
#define RC34   11
#define RC35   16
#define RC36   23
#define RC37    4
#define RC38   11
#define RC39   16
#define RC40   23
#define RC41    4
#define RC42   11
#define RC43   16
#define RC44   23
#define RC45    4
#define RC46   11
#define RC47   16
#define RC48   23

#define RC49    6
#define RC50   10
#define RC51   15
#define RC52   21
#define RC53    6
#define RC54   10
#define RC55   15
#define RC56   21
#define RC57    6
#define RC58   10
#define RC59   15
#define RC60   21
#define RC61    6
#define RC62   10
#define RC63   15
#define RC64   21

/*
 * Knn is the constant for for step nn.
 */

#define K1    C32(0xD76AA478)
#define K2    C32(0xE8C7B756)
#define K3    C32(0x242070DB)
#define K4    C32(0xC1BDCEEE)
#define K5    C32(0xF57C0FAF)
#define K6    C32(0x4787C62A)
#define K7    C32(0xA8304613)
#define K8    C32(0xFD469501)
#define K9    C32(0x698098D8)
#define K10   C32(0x8B44F7AF)
#define K11   C32(0xFFFF5BB1)
#define K12   C32(0x895CD7BE)
#define K13   C32(0x6B901122)
#define K14   C32(0xFD987193)
#define K15   C32(0xA679438E)
#define K16   C32(0x49B40821)

#define K17   C32(0xF61E2562)
#define K18   C32(0xC040B340)
#define K19   C32(0x265E5A51)
#define K20   C32(0xE9B6C7AA)
#define K21   C32(0xD62F105D)
#define K22   C32(0x02441453)
#define K23   C32(0xD8A1E681)
#define K24   C32(0xE7D3FBC8)
#define K25   C32(0x21E1CDE6)
#define K26   C32(0xC33707D6)
#define K27   C32(0xF4D50D87)
#define K28   C32(0x455A14ED)
#define K29   C32(0xA9E3E905)
#define K30   C32(0xFCEFA3F8)
#define K31   C32(0x676F02D9)
#define K32   C32(0x8D2A4C8A)

#define K33   C32(0xFFFA3942)
#define K34   C32(0x8771F681)
#define K35   C32(0x6D9D6122)
#define K36   C32(0xFDE5380C)
#define K37   C32(0xA4BEEA44)
#define K38   C32(0x4BDECFA9)
#define K39   C32(0xF6BB4B60)
#define K40   C32(0xBEBFBC70)
#define K41   C32(0x289B7EC6)
#define K42   C32(0xEAA127FA)
#define K43   C32(0xD4EF3085)
#define K44   C32(0x04881D05)
#define K45   C32(0xD9D4D039)
#define K46   C32(0xE6DB99E5)
#define K47   C32(0x1FA27CF8)
#define K48   C32(0xC4AC5665)

#define K49   C32(0xF4292244)
#define K50   C32(0x432AFF97)
#define K51   C32(0xAB9423A7)
#define K52   C32(0xFC93A039)
#define K53   C32(0x655B59C3)
#define K54   C32(0x8F0CCC92)
#define K55   C32(0xFFEFF47D)
#define K56   C32(0x85845DD1)
#define K57   C32(0x6FA87E4F)
#define K58   C32(0xFE2CE6E0)
#define K59   C32(0xA3014314)
#define K60   C32(0x4E0811A1)
#define K61   C32(0xF7537E82)
#define K62   C32(0xBD3AF235)
#define K63   C32(0x2AD7D2BB)
#define K64   C32(0xEB86D391)

/*
 * RMnn maps to the variable holding the message word for step nn.
 * Message words are in variables X0 to X15.
 */

#define RM1    X0
#define RM2    X1
#define RM3    X2
#define RM4    X3
#define RM5    X4
#define RM6    X5
#define RM7    X6
#define RM8    X7
#define RM9    X8
#define RM10   X9
#define RM11   X10
#define RM12   X11
#define RM13   X12
#define RM14   X13
#define RM15   X14
#define RM16   X15

#define RM17   X1
#define RM18   X6
#define RM19   X11
#define RM20   X0
#define RM21   X5
#define RM22   X10
#define RM23   X15
#define RM24   X4
#define RM25   X9
#define RM26   X14
#define RM27   X3
#define RM28   X8
#define RM29   X13
#define RM30   X2
#define RM31   X7
#define RM32   X12

#define RM33   X5
#define RM34   X8
#define RM35   X11
#define RM36   X14
#define RM37   X1
#define RM38   X4
#define RM39   X7
#define RM40   X10
#define RM41   X13
#define RM42   X0
#define RM43   X3
#define RM44   X6
#define RM45   X9
#define RM46   X12
#define RM47   X15
#define RM48   X2

#define RM49   X0
#define RM50   X7
#define RM51   X14
#define RM52   X5
#define RM53   X12
#define RM54   X3
#define RM55   X10
#define RM56   X1
#define RM57   X8
#define RM58   X15
#define RM59   X6
#define RM60   X13
#define RM61   X4
#define RM62   X11
#define RM63   X2
#define RM64   X9

/*
 * We will need to compute subtractions with the preprocessor,
 * resulting in decimal constants which are then concatenated to
 * prefixes to build macro names. This requires some devious
 * macro handling, with an explicit decrementation table.
 */

#define D1(n)    D1_(n)
#define D1_(n)   D1tab ## n
#define D2(n)    D1(D1(n))
#define D3(n)    D1(D1(D1(n)))
#define D4(n)    D1(D1(D1(D1(n))))
#define D5(n)    D1(D1(D1(D1(D1(n)))))

#define D1tabM3   M4
#define D1tabM2   M3
#define D1tabM1   M2
#define D1tab0    M1
#define D1tab1    0
#define D1tab2    1
#define D1tab3    2
#define D1tab4    3
#define D1tab5    4
#define D1tab6    5
#define D1tab7    6
#define D1tab8    7
#define D1tab9    8
#define D1tab10   9
#define D1tab11   10
#define D1tab12   11
#define D1tab13   12
#define D1tab14   13
#define D1tab15   14
#define D1tab16   15
#define D1tab17   16
#define D1tab18   17
#define D1tab19   18
#define D1tab20   19
#define D1tab21   20
#define D1tab22   21
#define D1tab23   22
#define D1tab24   23
#define D1tab25   24
#define D1tab26   25
#define D1tab27   26
#define D1tab28   27
#define D1tab29   28
#define D1tab30   29
#define D1tab31   30
#define D1tab32   31
#define D1tab33   32
#define D1tab34   33
#define D1tab35   34
#define D1tab36   35
#define D1tab37   36
#define D1tab38   37
#define D1tab39   38
#define D1tab40   39
#define D1tab41   40
#define D1tab42   41
#define D1tab43   42
#define D1tab44   43
#define D1tab45   44
#define D1tab46   45
#define D1tab47   46
#define D1tab48   47
#define D1tab49   48
#define D1tab50   49
#define D1tab51   50
#define D1tab52   51
#define D1tab53   52
#define D1tab54   53
#define D1tab55   54
#define D1tab56   55
#define D1tab57   56
#define D1tab58   57
#define D1tab59   58
#define D1tab60   59
#define D1tab61   60
#define D1tab62   61
#define D1tab63   62
#define D1tab64   63

/*
 * Qm1(nn) evaluates to the Qxx variable where xx is the decimal constant
 * which numerically immmediately precedes nn. Qm2(), Qm3()... compute
 * further preprocessor-based subtractions.
 */

#define XCAT(x, y)    XCAT_(x, y)
#define XCAT_(x, y)   x ## y
#define Qm1(n)    XCAT(Q, D1(n))
#define Qm2(n)    XCAT(Q, D2(n))
#define Qm3(n)    XCAT(Q, D3(n))
#define Qm4(n)    XCAT(Q, D4(n))

/*
 * Compute Q(n) from the four previous values and the message block.
 */
#define MKQ(n, qm1, qm2, qm3, qm4)   T32(qm1 + RL32(T32(RF ## n( \
                                     qm1, qm2, qm3) + qm4 + RM ## n \
                                     + K ## n), RC ## n))

#define SETQ(n)    (Q ## n = MKQ(n, Qm1(n), Qm2(n), Qm3(n), Qm4(n)))

/*
 * Compute RM(n) from Q(k) for n-4 <= k <= n.
 */
#define SETX(n)    (RM ## n = T32(RR32(T32(Q ## n - Qm1(n)), RC ## n) \
                   - K ## n - Qm4(n) - RF ## n(Qm1(n), Qm2(n), Qm3(n))))

/*
 * The initial values for a standard MD5.
 */
#define Q0_INIT    C32(0xEFCDAB89)
#define QM1_INIT   C32(0x98BADCFE)
#define QM2_INIT   C32(0x10325476)
#define QM3_INIT   C32(0x67452301)

/*
 * A custom PRNG. It is not a cryptographically secure PRNG, but our
 * needs are quite low here, both in terms of randomness (we just
 * want to avoid some kind of resonance with MD5 structures) and
 * performance (thanks to tunnels, speed of the PRNG is mostly
 * irrelevant).
 *
 * A reproducible PRNG (as opposed to using a system RNG, e.g.
 * libc's random() function) allows us to compare results with
 * other compilers and architectures.
 *
 * This specific PRNG is copied from Klima's code.
 */

typedef struct {
	sph_u32 a, b;
} rng_context;

static sph_u32
rng_next(rng_context *rc)
{
	sph_u32 t;

	t = rc->a;
	rc->a = (t & 1) ? ((t >> 1) ^ C32(0xF5000000)) : (t >> 1);
	rc->b = C32(1664525) * rc->b + C32(1013904223);
	return T32(rc->a + rc->b);
}

static void
rng_addseed(rng_context *rc, char *seed)
{
	for (;;) {
		sph_u32 c;

		c = *(unsigned char *)(seed ++);
		if (c == 0)
			break;
		rng_next(rc);
		rc->a ^= c;
		rc->b = T32(rc->b + c);
	}
}

static void
rng_init(rng_context *rc, char *seed)
{
	rc->a = C32(0xD3C4ADE6);
	rc->b = C32(0x1B519B92);
	rng_addseed(rc, seed);
}

/*
 * This function precomputes some bit patterns which are used to
 * explore a tunnel.
 */
static void
init_patterns(sph_u32 *p, sph_u32 mask)
{
	int ranks[32];
	int i, num;
	sph_u32 u;

	/*
	 * Count set bits in mask, and record their rank.
	 */
	num = 0;
	for (i = 0; i < 32; i ++) {
		sph_u32 x;

		x = C32(1) << i;
		if (mask & x)
			ranks[num ++] = i;
	}

	/*
	 * Compute patterns.
	 */
	for (u = 0; u < (C32(1) << num); u ++) {
		sph_u32 z;

		z = 0;
		for (i = 0; i < num; i ++)
			if (u & (C32(1) << i))
				z |= C32(1) << ranks[i];
		p[u] = z;
	}
}

static sph_u32 patterns_Q10[8];
static sph_u32 patterns_Q20[64];
static sph_u32 patterns_Q13[4096];
static sph_u32 patterns_Q14[512];
static sph_u32 patterns_Q9[8];
static sph_u32 patterns2_Q4[64];
static sph_u32 patterns2_Q9[256];

/*
 * This function shall be called once, before running the attack.
 * It initializes the patterns*[] arrays with bit patterns corresponding
 * to various tunnels.
 */
static void
init_tabs(void)
{
	init_patterns(patterns_Q10, C32(0x05000400));
	init_patterns(patterns_Q20, C32(0x00A04203));
	init_patterns(patterns_Q13, C32(0x18700E56));
	init_patterns(patterns_Q14, C32(0x1C000077));
	init_patterns(patterns_Q9, C32(0x00E00000));
	init_patterns(patterns2_Q4, C32(0x01C0E000));
	init_patterns(patterns2_Q9, C32(0x0074041C));
}

/*
 * Differential path for first block:
 *
 * Q[ 1]         = .... .... .... .... .... .... .... ....
 * Q[ 2]         = .... .... .... .... .... .... .... ....
 * Q[ 3]         = .... .... .vvv 0vvv vvvv 0vvv v0.. ....
 * Q[ 4]         = 1... .... 0^^^ 1^^^ ^^^^ 1^^^ ^011 ....
 * Q[ 5]         = 1000 100v 0100 0000 0000 0000 0010 v1v1
 * Q[ 6]         = 0000 001^ 0111 1111 1011 1100 0100 ^0^1
 * Q[ 7]         = 0000 0011 1111 1110 1111 1000 0010 0000
 * Q[ 8]         = 0000 0001 1..1 0001 0.0v 0101 0100 0000
 * Q[ 9]         = 1111 1011 ...1 0000 0.1^ 1111 0011 1101
 * Q[10]         = 0111 .... 0001 1111 1v01 ...0 01.. ..00
 * Q[11]         = 0010 .0v0 111. 0001 1^00 .0.0 11.. ..10
 * Q[12]         = 000. ..^^ .... 1000 0001 ...1 0... ....
 * Q[13]         = 01.. ..01 .... 1111 111. ...0 0... 1...
 * Q[14]         = 000. ..00 .... 1011 111. ...1 1... 1...
 * Q[15]         = v110 0001 ..V. .... 10.. .... .000 0000
 * Q[16]         = ^010 00.. ..A. .... v... .... .000 v000
 * Q[17]         = ^1v. .... .... ..0. ^... .... .... ^...
 * Q[18]         = ^.^. .... .... ..1. .... .... .... ....
 * Q[19]         = ^... .... .... ..0. .... .... .... ....
 * Q[20]         = ^... .... .... ..v. .... .... .... ....
 * Q[21]         = ^... .... .... ..^. .... .... .... ....
 * Q[22]         = ^... .... .... .... .... .... .... ....
 * Q[23]         = 0... .... .... .... .... .... .... ....
 * Q[24]         = 1... .... .... .... .... .... .... ....
 * Note: ^  = a bit, equal to the bit above
 *       v  = a bit, equal to the bit below
 *       V  = a bit, equal to the negation of the bit below
 *       A  = a bit, equal to the negation of the bit above
 *       .  = an arbitrary bit
 *      0/1 = a bit, which is fixed to 0 or 1
 */

/*
 * Using the specified seed to initialize the RNG, compute the first
 * blocks for the colliding pair. The first block for the first (resp.
 * second) message (64 bytes) is copied to block1[] (resp. block2[]).
 * The resulting chaining values are copied to fh1[] (resp. fh2[]).
 *
 * The blocks are such that:
 * fh2[0] - fh1[0] = 0x80000000
 * fh2[1] - fh1[1] = 0x82000000
 * fh2[2] - fh1[2] = 0x82000000
 * fh2[3] - fh1[3] = 0x82000000
 *
 * If "verbose" is non-zero, then this function prints out some dots
 * to indicate its progression (one dot for each pair of blocks which
 * match the differential path).
 */
static unsigned long long
first_block(char *seed, sph_u32 iv[4],
	unsigned char block1[64], unsigned char block2[64],
	sph_u32 fh1[4], sph_u32 fh2[4], int verbose)
{

#define Q0    (iv[1])
#define QM1   (iv[2])
#define QM2   (iv[3])
#define QM3   (iv[0])

#define RNG()   rng_next(&rc)
#define MK1(mask0, mask1)             ((RNG() & ~C32(mask0)) | C32(mask1))

/*
 * Get a random word, with masks for zeroes and ones, and also get some
 * bits from a previous value (with a mask). The bits from the previous
 * value are combined with XOR, hence we can get either plain or reversed
 * copies of each bit.
 */
#define MK2(mask0, mask1, p, maskp)   (MK1(mask0, mask1) ^ ((p) & C32(maskp)))

#define TT0(v, mask)     (((v) & C32(mask)) == 0)
#define TT1(v, mask)     (((v) & C32(mask)) == C32(mask))
#define TTH(v, w, mask)  ((((v) ^ (w)) & C32(mask)) == 0)

	rng_context rc;
	unsigned long long ccount;

	if (verbose) {
		printf("block 1: ");
		fflush(stdout);
	}
	rng_init(&rc, seed);
	ccount = 0;
	for (;;) {
		sph_u32 Q1, Q2, Q3, Q4, Q5, Q6, Q7, Q8, Q9, Q10;
		sph_u32 Q11, Q12, Q13, Q14, Q15, Q16, Q17, Q18, Q19, Q20;
		sph_u32 Q21, Q22, Q23, Q24, Q25, Q26, Q27, Q28, Q29, Q30;
		sph_u32 Q31, Q32, Q33, Q34, Q35, Q36, Q37, Q38, Q39, Q40;
		sph_u32 Q41, Q42, Q43, Q44, Q45, Q46, Q47, Q48, Q49, Q50;
		sph_u32 Q51, Q52, Q53, Q54, Q55, Q56, Q57, Q58, Q59, Q60;
		sph_u32 Q61, Q62, Q63, Q64;
		sph_u32 X0, X1, X2, X3, X4, X5, X6, X7, X8, X9;
		sph_u32 X10, X11, X12, X13, X14, X15;

		/* Q[ 1] = .... .... .... .... .... .... .... .... */
		Q1 = RNG();

		/* Q[ 2] = .... .... .... .... .... .... .... .... */
		/* delayed */

		/* Q[ 3] = .... .... .vvv 0vvv vvvv 0vvv v0.. .... */
		Q3 = MK1(0x00080840, 0x00000000);

		/* Q[ 4] = 1... .... 0^^^ 1^^^ ^^^^ 1^^^ ^011 .... */
		Q4 = MK2(0x00FFFFF0, 0x80080830, Q3, 0x0077F780);

		/* Q[ 5] = 1000 100v 0100 0000 0000 0000 0010 0101 */
		/*
		 * Note: last quartet is v1v1 in the "normal"
		 * differential path, but setting it to 0101 is
		 * necessary to allow the "dynamic tunnel" on Q[14]
		 * to operate correctly.
		 */
		Q5 = MK1(0x76BFFFDA, 0x88400025);

		/* Q[ 6] = 0000 001^ 0111 1111 1011 1100 0100 ^0^1 */
		Q6 = (Q5 & C32(0x0100000A)) | C32(0x027FBC41);

		/* Q[ 7] = 0000 0011 1111 1110 1111 1000 0010 0000 */
		Q7 = C32(0x03FEF820);

		/* Q[ 8] = 0000 0001 1..1 0001 0.0v 0101 0100 0000 */
		Q8 = MK1(0xFE0EAABF, 0x01910540);

		/* Q[ 9] = 1111 1011 ...1 0000 0.1^ 1111 0011 1101 */
		Q9 = MK2(0x040F90C2, 0xFB102F3D, Q8, 0x00001000);

		/* Q[10] = 0111 .... 0001 1111 1v01 ...0 01.. ..00 */
		Q10 = MK1(0x80E02183, 0x701F9040);

		/* Q[11] = 0010 .0v0 111. 0001 1^00 .0.0 11.. ..10 */
		Q11 = MK2(0xD50E7501, 0x20E180C2, Q10, 0x00004000);

		/* Q[12] = 000. ..^^ .... 1000 0001 ...1 0... .... */
		Q12 = MK2(0xE307E080, 0x00081100, Q11, 0x03000000);

		/* Q[13] = 01.. ..01 .... 1111 111. ...0 0... 1... */
		Q13 = MK1(0x82000180, 0x410FE008);

		/* Q[14] = 000. ..00 .... 1011 111. ...1 1... 1... */
		Q14 = MK1(0xE3040000, 0x000BE188);

		/* Q[15] = v110 0001 ..V. .... 10.. .... .000 0000 */
		Q15 = MK1(0x1E00407F, 0x61008000);

		/* Q[16] = ^010 00.. ..A. .... v... .... .000 v000 */
		Q16 = MK2(0xDC000077, 0x20200000, Q15, 0x80200000);

		/* Q[17] = ^1v. .... .... ..0. ^... .... .... ^... */
		Q17 = MK2(0x80028008, 0x40000000, Q16, 0x80008008);

		/*
		 * From Q[13..17], we can get X[17] (which is X[2]), which
		 * in turns defines our Q[2] (which we skipped previously).
		 */
		SETX(17);
		SETQ(2);

		/*
		 * At that point, we have Q[1] to Q[17], and X[2] is
		 * already computed. The X[i] (i != 2) are all computable
		 * deterministically, but we will do it only if we pass
		 * the next few tests on Q[18..24].
		 */

		/* Q[18] = ^.^. .... .... ..1. .... .... .... .... */
		SETX(7);
		SETQ(18);
		/*
		 * Q18 bits 29 and 31 must match those of Q17, and bit 17
		 * must be 1; but bit 17 of Q17 is guaranteed to be 0.
		 */
		if (((Q18 ^ Q17) & C32(0xA0020000)) != C32(0x00020000))
			continue;

		/* Q[19] = ^... .... .... ..0. .... .... .... .... */
		SETX(12);
		SETQ(19);
		if (((Q19 ^ Q18) & C32(0x80020000)) != C32(0x00020000))
			continue;
		/*
		 * Also, minor condition: 15 consecutive bits in an
		 * intermediate value computed for Q[19] must not be
		 * all equal to 1.
		 */
		if ((T32(Q19 - Q18) & C32(0xFFFE0000)) == C32(0xFFFE0000))
			continue;

		/* Q[20] = ^... .... .... ..v. .... .... .... .... */
		SETX(1);
		SETQ(20);
		if (!TTH(Q20, Q19, 0x80000000))
			continue;
		/*
		 * Another minor condition: three specific bits must not
		 * be all equal to 0.
		 */
		if ((T32(Q20 - Q19) & C32(0x000E0000)) == 0)
			continue;

#define SET_TEST_Q21   { \
		SETQ(21); \
		if (!TTH(Q21, Q20, 0x80020000)) \
			continue; \
	}
#define SET_TEST_Q22   { \
		SETQ(22); \
		if (!TTH(Q22, Q21, 0x80000000)) \
			continue; \
	}
#define SET_TEST_Q23   { \
		SETQ(23); \
		if (!TT0(Q23, 0x80000000)) \
			continue; \
		if (!TT0(T32(Q23 - Q22), 0x80000000)) \
			continue; \
	}
#define SET_TEST_Q24   { \
		SETQ(24); \
		if (!TT1(Q24, 0x80000000)) \
			continue; \
	}

		/* Q[21] = ^... .... .... ..^. .... .... .... .... */
		SETX(6);
		SET_TEST_Q21;

		/* Q[22] = ^... .... .... .... .... .... .... .... */
		SETX(11);
		SET_TEST_Q22;

		/* Q[23] = 0... .... .... .... .... .... .... .... */
		/*
		 * Q[23] is computed by adding a value to Q[22]; that
		 * added value should have its highest bit cleared.
		 */
		SETX(16);
		SET_TEST_Q23;

		/* Q[24] = 1... .... .... .... .... .... .... .... */
		SETX(5);
		SET_TEST_Q24;

		/* 
		 * These are not computed yet...
		 *
		SETX(3);
		SETX(4);
		SETX(8);
		SETX(9);
		SETX(10);
		SETX(13);
		SETX(14);
		SETX(15);
		 */

		/*
		 * A "tunnel" is a set of bits in a given Q[] word which
		 * we can easily alter, i.e.:
		 * -- this modification does not contradict, in itself,
		 * the differential path;
		 * -- the damage is "controlled", i.e. it is easy to modify
		 * some message blocks to get back to the "point of
		 * verification", namely after Q[24].
		 *
		 * Tunnel on Q[10] consists of bits 10, 24 and 26 (+1 in
		 * Klima's paper). This alters X[10] (not computed yet,
		 * so no big deal) and X[11], but not X[12] (because F()
		 * function is a mux and we choose bits in Q[11] so that
		 * these bits of Q[10] are ignored when computing Q[12]).
		 *
		 * Tunnel on Q[20]: 6 bits. We must recompute X[20], which
		 * is X[1]. This alters Q[1], Q[2], X[6] and then Q[21..24].
		 */

#define TUNNEL_BEGIN(name)   { \
	size_t i ## name; \
	sph_u32 sav_ ## name = name; \
	for (i ## name = 0; i ## name < ((sizeof patterns_ ## name) \
		/ (sizeof(sph_u32))); i ## name ++) { \
		name = sav_ ## name ^ patterns_ ## name[i ## name]; {

#define TUNNEL_END(name)   } } name = sav_ ## name; }

		/*
		 * We cheat with indentation because each tunnel yields
		 * another level of imbrication. We do not split this
		 * code into several functions because we have a huge
		 * state which we really want to keep as a single set
		 * of local variables.
		 */
		TUNNEL_BEGIN(Q10)

			SETX(11);
			SET_TEST_Q22;
			SET_TEST_Q23;
			SET_TEST_Q24;

		TUNNEL_BEGIN(Q20)

			SETX(20);
			SETQ(1);
			SETQ(2);
			SETX(6);
			SET_TEST_Q21;
			SET_TEST_Q22;
			SET_TEST_Q23;
			SET_TEST_Q24;

		TUNNEL_BEGIN(Q13)

			sph_u32 hq3, hq4, hq14, const_q14;
			sph_u32 save_Q3, save_Q4, save_Q14;
			int i;

			SETX(17);
			SETQ(2);
			SETX(6);
			SET_TEST_Q21;
			SET_TEST_Q22;
			SETX(16);
			SET_TEST_Q23;
			SETX(5);
			SET_TEST_Q24;

		/*
		 * Next tunnel is "dynamic" on Q[3], Q[4] and Q[14].
		 */
		hq3 = Q3 & C32(0x77FFFFDA);
		hq4 = Q4 & C32(0x8BFFFFF5);
		hq14 = Q14 & C32(0xE3FFFF88);
		const_q14 = T32(RR32(T32(Q7 - Q6), 17) - C32(0xA8304613)
			- RR32(T32(Q18 - Q17), 9) + C32(0xC040B340)
			+ G(Q17, Q16, Q15) - F(Q6, Q5, hq4) - hq3 + hq14);
		save_Q3 = Q3;
		save_Q4 = Q4;
		save_Q14 = Q14;
		for (i = 0; i < 512; i ++) {
			sph_u32 m14, z;

			m14 = patterns_Q14[i];
			z = T32(const_q14 + m14);
			if ((z & C32(0x03FFFFD0)) != 0)
				continue;
			Q4 = T32((z & C32(0x7400000A)) + hq4);
			Q3 = T32((z & C32(0x88000025)) + hq3);
			SETX(3);
			SETX(5);
			SET_TEST_Q24;
			Q14 = T32(hq14 + m14);

			SETX(7);
			SETX(4);
			SETX(8);
			SETX(14);
			SETX(15);

			TUNNEL_BEGIN(Q9)

			sph_u32 A, B, C, D;
			sph_u32 m1[16], m2[16], v1[4], v2[4];

			/*
			 * Here begins the really heavy sequence, which
			 * is computed again and again. We get through
			 * it about half a billion times. Each subsequent
			 * test rejects about half of the candidates.
			 */
			ccount ++;
			SETX(9);
			SETX(10);
			SETX(13);

			SETQ(25);
			SETQ(26);
			SETQ(27);
			SETQ(28);
			SETQ(29);
			SETQ(30);
			SETQ(31);
			SETQ(32);
			SETQ(33);
			SETQ(34);
			SETQ(35);
			if (!TT0(T32(Q35 - Q34), 0x80000000))
				continue;
			SETQ(36);
			SETQ(37);
			SETQ(38);
			SETQ(39);
			SETQ(40);
			SETQ(41);
			SETQ(42);
			SETQ(43);
			SETQ(44);
			SETQ(45);
			SETQ(46);
			SETQ(47);
			SETQ(48);
			if (!TTH(Q48, Q46, 0x80000000))
				continue;
			SETQ(49);
			if (!TTH(Q49, Q47, 0x80000000))
				continue;
			SETQ(50);
			if (TTH(Q50, Q46, 0x80000000))
				continue;
			SETQ(51);
			if (!TTH(Q51, Q47, 0x80000000))
				continue;
			SETQ(52);
			if (TTH(Q52, Q46, 0x80000000))
				continue;
			SETQ(53);
			if (!TTH(Q53, Q47, 0x80000000))
				continue;
			SETQ(54);
			if (TTH(Q54, Q46, 0x80000000))
				continue;
			SETQ(55);
			if (!TTH(Q55, Q47, 0x80000000))
				continue;
			SETQ(56);
			if (TTH(Q56, Q46, 0x80000000))
				continue;
			SETQ(57);
			if (!TTH(Q57, Q47, 0x80000000))
				continue;
			SETQ(58);
			if (TTH(Q58, Q46, 0x80000000))
				continue;
			SETQ(59);
			if (!TTH(Q59, Q47, 0x80000000))
				continue;
			SETQ(60);
			if (!TTH(Q60, Q46, 0x80000000))
				continue;
			if (!TT0(Q60, 0x02000000))
				continue;
			SETQ(61);
			if (!TTH(Q61, Q47, 0x80000000))
				continue;
			if (!TT1(Q61, 0x02000000))
				continue;
			SETQ(62);
			SETQ(63);
			SETQ(64);

			/*
			 * At that point, we have the complete block.
			 * We must still check that addition with the
			 * chain values (the IV, at that point) yields
			 * the proper bit values (8 bits are checked);
			 * this is needed also for the second block.
			 * If yes, then we may try the block with the
			 * difference, which has a 1/16 probability of
			 * giving the expected output difference.
			 */
			A = T32(Q61 + QM3);
			D = T32(Q62 + QM2);
			C = T32(Q63 + QM1);
			B = T32(Q64 + Q0);
			if (!TT0(D, 0x02000000))
				continue;
			if (!TT1(C, 0x02000000))
				continue;
			if (!TT0(C, 0x04000000))
				continue;
			if (!TTH(C, D, 0x80000000))
				continue;
			if (!TT0(B, 0x06000020))
				continue;
			if (!TTH(B, C, 0x80000000))
				continue;

			/*
			 * We have a candidate. We mark progress, and we
			 * try it. Note that we recompute the
			 * compression function over the unmodified
			 * block, and check that we get the correct
			 * values, as a sanity check.
			 */
			if (verbose) {
				printf(".");
				fflush(stdout);
			}

			m1[0] = X0;
			m1[1] = X1;
			m1[2] = X2;
			m1[3] = X3;
			m1[4] = X4;
			m1[5] = X5;
			m1[6] = X6;
			m1[7] = X7;
			m1[8] = X8;
			m1[9] = X9;
			m1[10] = X10;
			m1[11] = X11;
			m1[12] = X12;
			m1[13] = X13;
			m1[14] = X14;
			m1[15] = X15;
			memcpy(m2, m1, sizeof m1);
			m2[4] = T32(m2[4] + C32(0x80000000));
			m2[11] = T32(m2[11] + C32(0x00008000));
			m2[14] = T32(m2[14] + C32(0x80000000));
			v1[0] = QM3;
			v1[1] = Q0;
			v1[2] = QM1;
			v1[3] = QM2;
			memcpy(v2, v1, sizeof v1);
			sph_md5_comp(m1, v1);
			sph_md5_comp(m2, v2);
			if (v1[0] != A || v1[1] != B
				|| v1[2] != C || v1[3] != D) {
				fprintf(stderr, "INTERNAL ERROR\n");
				exit(EXIT_FAILURE);
			}
			if (T32(v2[0] - v1[0]) == C32(0x80000000)
				&& T32(v2[1] - v1[1]) == C32(0x82000000)
				&& T32(v2[2] - v1[2]) == C32(0x82000000)
				&& T32(v2[3] - v1[3]) == C32(0x82000000)) {
				size_t u;

				if (verbose) {
					printf("found !\n");
					fflush(stdout);
				}
				for (u = 0; u < 16; u ++) {
					sph_enc32le(block1 + 4 * u, m1[u]);
					sph_enc32le(block2 + 4 * u, m2[u]);
				}
				memcpy(fh1, v1, sizeof v1);
				memcpy(fh2, v2, sizeof v2);
				return ccount;
			}

			TUNNEL_END(Q9)
		}
		Q3 = save_Q3;
		Q4 = save_Q4;
		Q14 = save_Q14;

		TUNNEL_END(Q13)
		TUNNEL_END(Q20)
		TUNNEL_END(Q10)
	}

#undef RNG
#undef MK1
#undef MK2
#undef TT0
#undef TT1
#undef TTH
#undef TUNNEL_BEGIN
#undef TUNNEL_END
#undef SET_TEST_Q21
#undef SET_TEST_Q22
#undef SET_TEST_Q23
#undef SET_TEST_Q24

#undef Q0
#undef QM1
#undef QM2
#undef QM3
}

/*
 * This function calls first_block(), using the standard MD5 initial
 * value for the "iv" parameter.
 */
static unsigned long long
first_block_stdiv(char *seed,
	unsigned char block1[64], unsigned char block2[64],
	sph_u32 fh1[4], sph_u32 fh2[4], int verbose)
{
	sph_u32 iv[4];

	iv[1] = Q0_INIT;
	iv[2] = QM1_INIT;
	iv[3] = QM2_INIT;
	iv[0] = QM3_INIT;
	return first_block(seed, iv, block1, block2, fh1, fh2, verbose);
}

/*
 * A simple function to compute the Hamming weight of a 32-bit word.
 * It is used to drive a tunnel exploration heuristic. It is not
 * optimized, which is not an issue at all here.
 */
static int
hamming_weight(sph_u32 x)
{
	int hw;

	hw = 0;
	while (x != 0) {
		if (x & 1U)
			hw ++;
		x >>= 1;
	}
	return hw;
}

/*
 * Differential path for second block:
 *
 * Q[ 1]        = ~Ivvv 010v vv1v vvv1 .vvv 0vvv vv0. ...v 
 * Q[ 2]        = ~I^^^ 110^ ^^0^ ^^^1 0^^^ 1^^^ ^^0v v00^ 
 * Q[ 3]        = ~I011 111. ..01 1111 1..0 1vv1 011^ ^111 
 * Q[ 4]        = ~I011 101. ..00 0100 ...0 0^^0 0001 0001 
 * Q[ 5]        =  I100 10.0 0010 1111 0000 1110 0101 0000 
 * Q[ 6]        =  I..0 0101 1110 ..10 1110 1100 0101 0110 
 * Q[ 7]        = ~I..1 0111 1.00 ..01 10.1 1110 00.. ..v1 
 * Q[ 8]        = ~I..0 0100 0.11 ..10 1..v ..11 111. ..^0 
 * Q[ 9]        = ~Ivv1 1100 0xxx .x01 0..^ .x01 110x xx01 
 * Q[10]        = ~I^^1 1111 1000 v011 1vv0 1011 1100 0000 
 * Q[11]        = ~Ivvv vvvv .111 ^101 1^^0 0111 11v1 1111 
 * Q[12]        = ~I^^^ ^^^^ .... 1000 0001 .... 1.^. .... 
 * Q[13]        =  I011 1111 0... 1111 111. .... 0... 1... 
 * Q[14]        =  I100 0000 1... 1011 111. .... 1... 1... 
 * Q[15]        =  0111 1101 .... ..10 00.. .... .... 0... 
 * Q[16]        =  ^.10 .... .... ..01 1... .... .... 1... 
 *
 * with:
 *   I = most significant bit of B (second word of chaining variables)
 *   x = same as '.' (documented in Klima's code as preparation for
 *       an unimplemented tunnel)
 */

/*
 * Compute the second blocks for the colliding pairs, using the provided
 * RNG seed. The second block of the first message (respectively second
 * message) is written out on block1[] (resp. block2[]). The chaining
 * values on input of that block are provided as fh1[] (resp. fh2[])
 * and should be the values computed by a call to first_block().
 *
 * If fh3 is not NULL, then it receives the colliding output (output of
 * the compression function immediately after the newly computed block).
 */
static void
second_block(char *seed, sph_u32 fh1[4], sph_u32 fh2[4],
	unsigned char block1[64], unsigned char block2[64],
	sph_u32 fh3[4], int verbose)
{
#define Q0    (fh1[1])
#define QM1   (fh1[2])
#define QM2   (fh1[3])
#define QM3   (fh1[0])

#define RNG()   rng_next(&rc)
#define MK1(mask0, mask1)             ((RNG() & ~C32(mask0)) | C32(mask1))

/*
 * Get a random word, with masks for zeroes and ones, and also get some
 * bits from a previous value (with a mask). The bits from the previous
 * value are combined with XOR, hence we can get either plain or reversed
 * copies of each bit.
 */
#define MK2(mask0, mask1, p, maskp)   (MK1(mask0, mask1) ^ ((p) & C32(maskp)))

#define TT0(v, mask)     (((v) & C32(mask)) == 0)
#define TT1(v, mask)     (((v) & C32(mask)) == C32(mask))
#define TTH(v, w, mask)  ((((v) ^ (w)) & C32(mask)) == 0)

#define LOOP_BEGIN(num)  { sph_u32 cc; for (cc = (num); cc > 0; cc --) {
#define LOOP_END         } }

#define TUNNEL2_BEGIN(name, mask)   { \
	size_t i ## name; \
	sph_u32 sav_ ## name = name; \
	for (i ## name = 0; i ## name < ((sizeof patterns2_ ## name) \
		/ (sizeof(sph_u32))); i ## name ++) { \
		name = (sav_ ## name & ~C32(mask)) \
			^ patterns2_ ## name[i ## name]; {

#define TUNNEL2_END(name)   } } name = sav_ ## name; }

	rng_context rc;

	if (verbose) {
		printf("block 2: ");
		fflush(stdout);
	}

	/*
	 * For the second block, we prefix the seed, so that we get
	 * an other random stream.
	 */
	rng_init(&rc, "second block: ");
	rng_addseed(&rc, seed);

	for (;;) {
		sph_u32 Q1, Q2, Q3, Q4, Q5, Q6, Q7, Q8, Q9, Q10;
		sph_u32 Q11, Q12, Q13, Q14, Q15, Q16, Q17, Q18, Q19, Q20;
		sph_u32 Q21, Q22, Q23, Q24, Q25, Q26, Q27, Q28, Q29, Q30;
		sph_u32 Q31, Q32, Q33, Q34, Q35, Q36, Q37, Q38, Q39, Q40;
		sph_u32 Q41, Q42, Q43, Q44, Q45, Q46, Q47, Q48, Q49, Q50;
		sph_u32 Q51, Q52, Q53, Q54, Q55, Q56, Q57, Q58, Q59, Q60;
		sph_u32 Q61, Q62, Q63, Q64;
		sph_u32 X0, X1, X2, X3, X4, X5, X6, X7, X8, X9;
		sph_u32 X10, X11, X12, X13, X14, X15;
		sph_u32 bit_I, bit_nI, MM, tq1, tq2;
		int hwMM;

		bit_I = fh1[1] & C32(0x80000000);
		bit_nI = bit_I ^ C32(0x80000000);

		/* Q[ 1] = ~Ivvv 010v vv1v vvv1 .vvv 0vvv vv0. ...v */
		Q1 = bit_nI | MK1(0x8A000820, 0x04210000);

		/* Q[ 2] = ~I^^^ 110^ ^^0^ ^^^1 0^^^ 1^^^ ^^0v v00^ */
		Q2 = bit_nI | MK2(0xF3FEF7E7, 0x0C010800, Q1, 0x71DE77C1);

		/* Q[ 3] = ~I011 111. ..01 1111 1..0 1vv1 011^ ^111 */
		Q3 = bit_nI | MK2(0xC0201098, 0x3E1F8967, Q2, 0x00000018);

		/* Q[ 4] = ~I011 101. ..00 0100 ...0 0^^0 0001 0001 */
		Q4 = bit_nI | MK2(0xC43B1FEE, 0x3A040011, Q3, 0x00000600);

		/* Q[ 5] =  I100 10.0 0010 1111 0000 1110 0101 0000 */
		Q5 = bit_I | MK1(0xB5D0F1AF, 0x482F0E50);

		/* Q[ 6] =  I..0 0101 1110 ..10 1110 1100 0101 0110 */
		Q6 = bit_I | MK1(0x9A1113A9, 0x05E2EC56);

		/* Q[ 7] = ~I..1 0111 1.00 ..01 10.1 1110 00.. ..v1 */
		Q7 = bit_nI | MK1(0x883241C0, 0x17819E01);

		/* Q[ 8] = ~I..0 0100 0.11 ..10 1..v ..11 111. ..^0 */
		Q8 = bit_nI | MK2(0x9B810003, 0x043283E0, Q7, 0x00000002);

		/* Q[ 9] = ~Ivv1 1100 0xxx .x01 0..^ .x01 110x xx01 */
		Q9 = bit_nI | MK2(0x83829222, 0x1C0101C1, Q8, 0x00001000);

		/* Q[10] = ~I^^1 1111 1000 v011 1vv0 1011 1100 0000 */
		Q10 = bit_nI | MK2(0xE074143F, 0x1F838BC0, Q9, 0x60000000);

		/* Q[11] = ~Ivvv vvvv .111 ^101 1^^0 0111 11v1 1111 */
		Q11 = bit_nI | MK2(0x800A7800, 0x007587DF, Q10, 0x00086000);

		/* Q[12] = ~I^^^ ^^^^ .... 1000 0001 .... 1.^. .... */
		Q12 = bit_nI | MK2(0xFF07E020, 0x00081080, Q11, 0x7F000020);

		/* Q[13] =  I011 1111 0... 1111 111. .... 0... 1... */
		Q13 = bit_I | MK1(0xC0800080, 0x3F0FE008);

		/* Q[14] =  I100 0000 1... 1011 111. .... 1... 1... */
		Q14 = bit_I | MK1(0xBF040000, 0x408BE088);

		MM = C32(0x71DE77C1) & ~(fh1[1] ^ fh1[2]);
		hwMM = hamming_weight(MM);
		tq1 = Q1 & ~MM;
		tq2 = Q2 & ~MM;

		/*
		 * We loop on the last two words. Klima's code tries 2^25
		 * times before giving up on the Q[1..14] initial values.
		 * We can filter out many attempts immediately; we keep
		 * about 1.3% of such values.
		 */
		LOOP_BEGIN(C32(1) << 25)

		sph_u32 save_Q1, save_Q2;

		/* Q[15] =  0111 1101 .... ..10 00.. .... .... 0... */
		Q15 = MK1(0x8201C008, 0x7D020000);

		/* Q[16] =  ^.10 .... .... ..01 1... .... .... 1... */
		Q16 = MK1(0x90020000, 0x20018008);

		SETX(2);
		SETQ(17);
		if (!TTH(Q17, Q16, 0x80028008))
			continue;
		if (TT1(T32(Q17 - Q16), 0xE0000000))
			continue;
		SETX(7);
		SETQ(18);
		if (((Q18 ^ Q17) & C32(0xA0020000)) != C32(0x00020000))
			continue;
		SETX(12);
		SETQ(19);
		if (TT1(T32(Q19 - Q18), 0xFFFE0000))
			continue;
		if (!TT0(Q19, 0x80020000))
			continue;
		SETX(11);
		SETX(16);

		/*
		 * At that point, we have Q[1..19], X[2], X[7], X[12],
		 * X[11] and X[16]. We now try variants of Q[1] and Q[2].
		 * We choose variants which do not impact X[2] (the modified
		 * bits are equal in Q[1] and Q[2], and are such that Q[0]
		 * and Q[-1] are also equal at those positions, which
		 * allows us to ignore the F function).
		 *
		 * This is directed by the MM mask. Klima's code loops
		 * 2^n times, where n is the Hamming weight of MM.
		 * FIXME: try to loop over the exact 2^n patterns, rather
		 * than using 2^n random patterns.
		 */
		save_Q1 = Q1;
		save_Q2 = Q2;
		LOOP_BEGIN(C32(1) << hwMM)

		Q1 = T32((RNG() & MM) + tq1);
		Q2 = T32((Q1 & MM) + tq2);
		SETX(1);
		SETQ(20);
		if (!TT0(Q20, 0x80000000))
			continue;
		if (TT0(T32(Q20 - Q19), 0x000E0000))
			continue;
		SETX(6);
		SETQ(21);
		if (!TTH(Q21, Q20, 0x80020000))
			continue;
		SETQ(22);
		if (!TT0(Q22, 0x80000000))
			continue;
		SETQ(23);
		if (!TT0(T32(Q23 - Q22), 0x80000000))
			continue;
		if (!TT0(Q23, 0x80000000))
			continue;
		SETX(5);
		SETQ(24);
		if (!TT1(Q24, 0x80000000))
			continue;
		SETX(3);
		SETX(14);
		SETX(15);

		/*
		 * Tunnel on Q[4]. Klima's code reconstructs the bit
		 * patterns directly rather than using precomputed
		 * patterns as in the first block. Here, we use a
		 * precomputed pattern.
		 */
		TUNNEL2_BEGIN(Q4, 0x01C0E000)

		SETX(5);
		SETQ(24);
		if (!TT1(Q24, 0x80000000))
			continue;
		SETX(4);
		SETX(8);

		/*
		 * Tunnel on Q[9].
		 */
		TUNNEL2_BEGIN(Q9, 0)

		sph_u32 m1[16], m2[16], v1[4], v2[4];

		SETX(9);
		SETX(10);
		SETX(13);

		SETQ(25);
		SETQ(26);
		SETQ(27);
		SETQ(28);
		SETQ(29);
		SETQ(30);
		SETQ(31);
		SETQ(32);
		SETQ(33);
		SETQ(34);
		SETQ(35);
		if (!TT1(T32(Q35 - Q34), 0x80000000))
			continue;
		SETQ(36);
		SETQ(37);
		SETQ(38);
		SETQ(39);
		SETQ(40);
		SETQ(41);
		SETQ(42);
		SETQ(43);
		SETQ(44);
		SETQ(45);
		SETQ(46);
		SETQ(47);
		SETQ(48);

#define SJ(x)    { if (!TTH(x, Q46, 0x80000000)) continue; }
#define SK(x)    { if (!TTH(x, Q47, 0x80000000)) continue; }
#define SnJ(x)   { if (TTH(x, Q46, 0x80000000)) continue; }

		SJ(Q48);
		SETQ(49);
		SK(Q49);
		SETQ(50);
		SnJ(Q50);
		SETQ(51);
		SK(Q51);
		SETQ(52);
		SnJ(Q52);
		SETQ(53);
		SK(Q53);
		SETQ(54);
		SnJ(Q54);
		SETQ(55);
		SK(Q55);
		SETQ(56);
		SnJ(Q56);
		SETQ(57);
		SK(Q57);
		SETQ(58);
		SnJ(Q58);
		SETQ(59);
		SK(Q59);
		SETQ(60);
		SJ(Q60);
		if (!TT0(Q60, 0x02000000))
			continue;
		SETQ(61);
		SK(Q61);
		if (!TT1(Q61, 0x02000000))
			continue;
		SETQ(62);
		SJ(Q62);
		if (!TT1(Q62, 0x02000000))
			continue;
		if (TT0(T32(Q62 - Q61), 0xFE000000))
			continue;
		SETQ(63);
		SK(Q63);
		if (!TT1(Q63, 0x02000000))
			continue;
		SETQ(64);
		if (!TT1(Q64, 0x02000000))
			continue;

#undef SJ
#undef SK
#undef SnJ

		if (verbose) {
			printf(".");
			fflush(stdout);
		}
		m1[0] = X0;
		m1[1] = X1;
		m1[2] = X2;
		m1[3] = X3;
		m1[4] = X4;
		m1[5] = X5;
		m1[6] = X6;
		m1[7] = X7;
		m1[8] = X8;
		m1[9] = X9;
		m1[10] = X10;
		m1[11] = X11;
		m1[12] = X12;
		m1[13] = X13;
		m1[14] = X14;
		m1[15] = X15;
		memcpy(m2, m1, sizeof m1);
		m2[4] = T32(m2[4] - C32(0x80000000));
		m2[11] = T32(m2[11] - C32(0x00008000));
		m2[14] = T32(m2[14] - C32(0x80000000));
		memcpy(v1, fh1, sizeof v1);
		memcpy(v2, fh2, sizeof v2);
		sph_md5_comp(m1, v1);

		/*
		 * Sanity check: did we compute MD5 correctly ?
		 */
		{
			sph_u32 A, B, C, D;

			A = T32(Q61 + QM3);
			D = T32(Q62 + QM2);
			C = T32(Q63 + QM1);
			B = T32(Q64 + Q0);
			if (v1[0] != A || v1[1] != B
				|| v1[2] != C || v1[3] != D) {
				fprintf(stderr, "INTERNAL ERROR\n");
			}
		}

		sph_md5_comp(m2, v2);
		if (v1[0] == v2[0] && v1[1] == v2[1]
			&& v1[2] == v2[2] && v1[3] == v2[3]) {
			size_t u;

			if (verbose) {
				printf("found !\n");
				fflush(stdout);
			}
			for (u = 0; u < 16; u ++) {
				sph_enc32le(block1 + 4 * u, m1[u]);
				sph_enc32le(block2 + 4 * u, m2[u]);
			}
			if (fh3 != NULL)
				memcpy(fh3, v1, sizeof v1);
			return;
		}

		TUNNEL2_END(Q9)
		TUNNEL2_END(Q4)
		LOOP_END
		Q1 = save_Q1;
		Q2 = save_Q2;
		LOOP_END
	}

#undef RNG
#undef MK1
#undef MK2
#undef TT0
#undef TT1
#undef TTH
#undef LOOP_BEGIN
#undef LOOP_END
#undef TUNNEL2_BEGIN
#undef TUNNEL2_END

#undef Q0
#undef QM1
#undef QM2
#undef QM3
}

/*
 * Main program: it generates plain MD5 collisions. It does not print
 * them out, but it measures how much time it spends on finding a
 * collision, and how many POV values it tried. Measures are
 * averaged as long as the program runs. On a 64-bit 2.4 GHz Intel
 * Core2 CPU, using a single core, and compiling this program with
 * a recent gcc and decent optimization flags (e.g. -O2), you may
 * expect an average of 14 seconds per collision (i.e. one line of
 * log every 14 seconds).
 *
 * The PRNG used is seed by the first program argument (a character
 * string, only the first 30 characters are used). Extra arguments are
 * ignored. If you run this program on several cores or systems, to
 * get more precise measures, then you MUST use distinct seeds for all
 * instances.
 */
#if 0
int
main(int argc, char *argv[])
{
	char seed[50];
	size_t seed_len;
	unsigned char m1[128], m2[128], r1[16], r2[16];
	sph_u32 fh1[4], fh2[4];
	sph_md5_context mc;
	unsigned long num;
	unsigned long long tcc;
	double ttime;

	init_tabs();
	if (argc < 2) {
		seed_len = 0;
	} else {
		seed_len = strlen(argv[1]);
		if (seed_len > (sizeof seed) - 20)
			seed_len = (sizeof seed) - 20;
		memcpy(seed, argv[1], seed_len);
	}
	num = 0;
	tcc = 0;
	ttime = 0.0;
	for (;;) {
		clock_t begin, end;
		unsigned long long ccount;

		sprintf(seed + seed_len, "%lu", num);
		begin = clock();
		ccount = first_block_stdiv(seed, m1, m2, fh1, fh2, 0);
		second_block(seed, fh1, fh2, m1 + 64, m2 + 64, 0, 0);
		end = clock();
		sph_md5_init(&mc);
		sph_md5(&mc, m1, sizeof m1);
		sph_md5_close(&mc, r1);
		sph_md5(&mc, m2, sizeof m2);
		sph_md5_close(&mc, r2);
		if (memcmp(m1, m2, sizeof m1) == 0) {
			fprintf(stderr, "INTERNAL ERROR: identical messages\n");
			exit(EXIT_FAILURE);
		}
		if (memcmp(r1, r2, sizeof r1) != 0) {
			fprintf(stderr, "INTERNAL ERROR: distinct outputs\n");
			exit(EXIT_FAILURE);
		}
		num ++;
		tcc += ccount;
		ttime += (end - begin) / (double)CLOCKS_PER_SEC;

		printf("%9lu collisions:  avg time: %7.2fs"
			"     avg ccount: %13.2f\n",
			num, ttime / (double)num, (double)tcc / (double)num);
		fflush(stdout);
	}
	return 0;
}
#endif
/*
 * Alternate main program.
 *
 * This one also takes a seed as argument. It generates pairs of files
 * for multicollisions. Specifically, it generate two hundred files,
 * names outXY_Z where X and Y are decimal digits, and Z is either 1 or
 * 2. Each file has length 128. For a given X and Y, the two files which
 * have XY in their name are distinct from each other.
 *
 * For a file outXY_Z, define its rank, which is equal to 10*X+Y (so
 * you have two distinct files for each rank). For an integer n between
 * 0 and 99, you build files consisting of the concatenation of n+1
 * outXY_Z files, using exactly one file of each rank from 0 to n,
 * in that order. Since you have exactly two files for a given rank
 * and they are distinct, you can build 2^(n+1) distinct files. All
 * those files will hash to the same value with MD5.
 *
 * For instance, if you choose n = 2, then you build eight files:
 *
 *   out00_1 || out01_1 || out02_1
 *   out00_2 || out01_1 || out02_1
 *   out00_1 || out01_2 || out02_1
 *   out00_2 || out01_2 || out02_1
 *   out00_1 || out01_1 || out02_2
 *   out00_2 || out01_1 || out02_2
 *   out00_1 || out01_2 || out02_2
 *   out00_2 || out01_2 || out02_2
 *
 * With the two hundred files, you have everything you need for building
 * 2^100 distinct files which hash to the same value by MD5.
 *
 * To use this program, comment out the main() function above, then
 * uncomment the write_file() and main() functions below.
 */
#if 1
static void
write_file(char *name, unsigned char *data, size_t len)
{
	FILE *f;

	f = fopen(name, "wb");
	if (f == NULL) {
		fprintf(stderr, "cannot open file '%s' for writing\n", name);
		exit(EXIT_FAILURE);
	}
	if (fwrite(data, 1, len, f) != len) {
		fprintf(stderr, "error writing to file '%s'\n", name);
		exit(EXIT_FAILURE);
	}
	fclose(f);
}

int
main(int argc, char *argv[])
{
	char *seed;
	int i;
	sph_u32 iv[4];

	init_tabs();
	if (argc < 2) {
		seed = "";
	} else {
		seed = argv[1];
	}
	iv[0] = C32(0x67452301);
	iv[1] = C32(0xEFCDAB89);
	iv[2] = C32(0x98BADCFE);
	iv[3] = C32(0x10325476);
	for (i = 0; i < 100; i ++) {
		unsigned char m1[128], m2[128];
		sph_u32 fh1[4], fh2[4];
		char fname[50];

		first_block(seed, iv, m1, m2, fh1, fh2, 0);
		second_block(seed, fh1, fh2, m1 + 64, m2 + 64, iv, 0);
		sprintf(fname, "out%02d_1", i);
		write_file(fname, m1, sizeof m1);
		sprintf(fname, "out%02d_2", i);
		write_file(fname, m2, sizeof m2);
		printf("%2d\n", i);
		fflush(stdout);
	}
	return 0;
}
#endif
